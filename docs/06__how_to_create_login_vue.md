---
marp: true
style: |
  section.frontpage {
    text-align: center;
  }
  section p, section li {
    font-size: 24px;
  }
---
<!-- _class: frontpage -->
# Vue.jsを用いたログイン画面の実装

---

## ・・・その前に

今回の構成では **Thymeleafは使いません。**
そのため各Controller.javaには、Thymeleafにより必要な値を送る設定は施しておりません。

### なぜThymeleafを使わないのか？

従来のSpring Boot開発（Thymeleaf使用）と今回の開発（Vue.js使用）の違いは以下のようになります。

* **Thymeleaf（サーバーサイド・レンダリング）**:
サーバー側でHTMLにデータを埋め込んで、完成した「画面」をブラウザに送ります。
* **Vue.js + @RestController（クライアントサイド・レンダリング）**:
サーバー（Java）は **「生データ（JSON）」** だけを返します。画面をどう組み立てるかは、ブラウザ側で動く **Vue.js** が担当します。

Java側で値を `model.addAttribute()` する代わりに、Vue.js側が `axios` を使ってAPIを叩き、返ってきたJSONを受け取って画面に表示させる、という役割分担になっています。

---

## ログインフォームの作成

それでは、最初の画面であるログインフォームを作りましょう。Vue.jsでは `.vue` というファイルに「見た目（HTML）」「動き（JS）」「装飾（CSS）」をまとめて書きます。

`frontend/src/components` フォルダに **`LoginForm.vue`** というファイルを新規作成し、以下のコードを記述してください。

```html
<template>
  <div class="login-container">
    <h2>勤怠管理システム ログイン</h2>
    <form @submit.prevent="handleLogin">
      <div class="field">
        <label>ユーザーID (Email)</label>
        <input type="email" v-model="userId" required placeholder="example@example.com">
      </div>
      <div class="field">
        <label>パスワード</label>
        <input type="password" v-model="password" required>
      </div>
```

---

```html
    <button type="submit">ログイン</button>
    </form>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import apiClient from '../api'; // 先ほど作った設定ファイルを読み込む

const userId = ref('');
const password = ref('');
const errorMessage = ref('');

const handleLogin = async () => {
  try {
    // curlで叩いていたAPIをここで呼び出す
    const response = await apiClient.post('/users/login', {
      userId: userId.value,
      password: password.value
    });
    
    console.log('ログイン成功:', response.data);
    alert(`ようこそ、${response.data.userName}さん！`);
    // 本来はここで打刻画面へ遷移させますが、まずは疎通確認まで
  } catch (error) {
    console.error('ログイン失敗:', error);
    errorMessage.value = 'ユーザーIDまたはパスワードが正しくありません。';
  }
};
</script>
```

---

```css
<style scoped>
.login-container {
  max-width: 400px;
  margin: 50px auto;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}
.field { margin-bottom: 15px; }
label { display: block; margin-bottom: 5px; }
input { width: 100%; padding: 8px; box-sizing: border-box; }
button { width: 100%; padding: 10px; background-color: #42b983; color: white; border: none; cursor: pointer; }
.error { color: red; margin-top: 10px; }
</style>
```

### 画面に表示させるための設定

作成した `LoginForm.vue` を実際にブラウザで見られるように、メインの `App.vue` を書き換えます。

`frontend/src/App.vue` の中身をすべて消して、以下に書き換えてください。

---

```html
<template>
  <div id="app">
    <LoginForm />
  </div>
</template>

<script setup>
import LoginForm from './components/LoginForm.vue';
</script>
```

### 動作確認のポイント

1. **バックエンドを起動** (`./mvnw spring-boot:run`)
2. **フロントエンドを起動** (`npm run dev`)
3. ブラウザでログイン画面が表示されたら、以前 `curl` で登録したメールアドレスとパスワードを入力して「ログイン」ボタンを押してみてください。

**「ようこそ、〇〇さん！」** とアラートが出れば、Vue.jsからJavaのAPIを呼び出すことに成功です！

---

## 画面経由のログイン失敗時の対処法

ログインに失敗してしまうのは、開発の現場では非常によくある「最初の壁」です。
原因を突き止めるために、まずは **H2 Databaseの中身を直接確認する** 操作から始めましょう。

ブラウザでデータベースの裏側を覗きに行く手順を詳しく解説します。

---

### H2Consoleへのアクセスのための事前準備

以下の手順で `SecurityConfig.java` を修正してみてください。
`securityFilterChain` メソッドの中身を以下のように更新します。

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/h2-console/**") // CSRF対策をH2Consoleだけ無効化（これがないとログインボタンが効かない）
            .disable() 
        )
        .headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin()) // H2 Consoleは内部でFramesetを使っているため、その表示を許可する
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/h2-console/**").permitAll() // H2 Consoleへのアクセスをすべての人に許可
            .requestMatchers("/api/users/register", "/api/users/login", "/api/attendance/**").permitAll()
            .anyRequest().authenticated()
        );

    return http.build();
}
```

---

### なぜこの設定が必要なのか？

デフォルト状態では、 **Spring SecurityがH2 Consoleへのアクセスも「守るべき対象」としてブロック** してしまい、そのままではアクセスできません。

* **CSRFの無効化**: H2 Consoleは独自のフォーム送信を行いますが、Spring SecurityのCSRF保護が有効だと「不正な送信」とみなされて拒否されます。
* **FrameOptions (sameOrigin)**: H2 Consoleの画面は「フレーム（画面分割）」構造になっています。Spring Securityはデフォルトで「クリックジャッキング対策」としてフレーム表示を禁止しているため、これを解除する必要があります。

H2 Consoleは通常のAPIとは異なり、専用の画面（コンソール）を持つため、`SecurityConfig.java` で特別に **「ここは通していいよ」** と **「フレーム表示を許可するよ」** という2つの許可設定を追加する必要があります。

---

### それでも解消しなかった場合の次なる対処法

`SecurityConfig.java` を正しく書き換えても解決しない場合、GitHub Codespacesという環境特有の、非常に「惜しい」ところに原因がある可能性が高いです。

主な原因は **「GitHubのプロキシ（URL保護）」** と **「Spring SecurityのCSRF判定」** の食い違いでしょう。

次のポイントを確認・修正してみてください。

#### ポートの「公開範囲」を Public にする

Codespacesでは、起動したポート（8080）はデフォルトで「Private（自分だけ）」に設定されています。これが原因で、ブラウザから特定のパス（`/h2-console`）を叩いた際に、GitHub側で認証エラーを出すことがあります。

1. VS Code内の **「PORTS」** タブ（ターミナルの隣あたり）を開きます。
2. `8080` 番ポートを右クリックします。
3. **「Port Visibility」** を **「Public」** に変更します。

---

#### SecurityConfig.java の CSRF 無効化範囲を広げる

Codespaces経由だと、URLが複雑（`*.github.dev`）で、Spring Securityが「外部からの不正な攻撃」と誤判定しがちです。開発環境に限定して、CSRF保護を完全に無効化して疎通を優先させましょう。

`SecurityConfig.java` を以下のように、より強力に開放する設定に書き換えてみてください。

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // 開発環境のため、一旦すべてのCSRFを無効化する（もっとも確実な回避策）
        .headers(headers -> headers
            .frameOptions(frame -> frame.disable()) // H2-consoleのために完全に無効化
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/h2-console/**").permitAll() // パス指定が間違っていないか再確認
            .requestMatchers("/api/users/register", "/api/users/login", "/api/attendance/**").permitAll()
            .anyRequest().authenticated()
        );

    return http.build();
}
```

---

### これ、見落としてませんか？

* ログを見る限りSpringBoot自体は正常に起動している。
* `/h2-console` は準備完了（Available）となっている。
* しかし、アクセス時に `403` が出る、かつ **ログに「Forbidden」などの警告が出ていない**

こういう場合、原因はJavaのコードの外側にある可能性が極めて高いです。

#### `application.properties` の記述の見直し

次の記述が残っていたのなら、**すぐに削除（またはコメントアウト）してください！**

```properties
# Spring Security のデフォルトログインを一時的に無効化（動作確認を優先するため）
# ※後ほど、しっかりとしたセキュリティ設定を書く際に削除します
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

はっきり述べましょう。これが **403エラーやログイン失敗の最大の原因** です！

---

#### なぜこの記述が問題なのか？

この設定は「Spring Securityの機能を丸ごとオフにする」という非常に強力なものです。これがあるせいで、以下の矛盾が発生しています。

1. **設定が無視されている**:
あなたが一生懸命 `SecurityConfig.java` に書いた許可設定（`/h2-console` を通す、CSRFを無効にするなど）が、**Spring Bootによって完全に無視されています。**
2. **H2 Console側のセキュリティと衝突している**:
Spring Securityをオフにしても、H2 Console自体には「外部からの接続を拒否する」という独自の安全策があります。SecurityConfigが機能していないため、その安全策を突破するための設定（`.frameOptions().disable()` など）が適用されず、結果として `403` が出てしまいます。
3. **ログイン処理の不整合**:
バックエンドのコード内で `PasswordEncoder` などを使っていますが、本体のセキュリティ機能がオフだと、認証プロセスが正しく初期化されず、正常なパスワード照合ができない場合があります。

---

### 今すぐ行うべき修正手順

#### 1. `application.properties` を修正

該当の行を削除するか、先頭に `#` をつけて無効化してください。

```properties
# 以下の行を削除またはコメントアウト
# spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

# ローカル環境以外（Codespacesのプロキシ経由など）からのブラウザアクセスを明示的に許可
spring.h2.console.settings.web-allow-others=true
```

#### 2. `SecurityConfig.java` を確認

以前作成した「全部許可」に近い設定になっていることを確認してください。

#### 3. Spring Boot を再起動

これが一番重要です。設定を読み込ませるために、必ず再起動してください。

---

### 修正後の動作確認フロー

再起動後、以下の順番で確認してみてください。

1. **H2 Consoleにアクセス** : `https://.../h2-console` が開けるか確認します。

2. **ユーザー登録（curl）** : 改めて `test-user@example.com` を登録します。
3. **H2 Consoleで確認** : `SELECT * FROM APPLY_USER_ACCOUNTS;` を実行し、データが存在することを目視で確認します。
4. **Vue.js画面でログイン** : ブラウザの画面からログインを試します。

この「オートコンフィギュレーションの除外」を外すことで、ようやくあなたが書いた `SecurityConfig.java` に命が吹き込まれます。

---

### ログインできない問題への「別のアプローチ」

もしどうしても H2 Console が開けない場合、データベースの中身を確認するために **「一時的な確認用API」** を Java で作ってしまうのも手です。

H2 Console の `403` は環境起因で手間取りがちで時間の浪費につながるため、もし上記を試してもダメな場合は、この **「デバッグ用API」でデータ生存確認を行う** 方へ切り替えるのも賢い選択です。

例えば、`UserAccountController` に以下の記述を追加し、併せて `UserAccountService` にも対応する記述を追加したうえで、SpringBootを再起動します。

```java
@GetMapping("/debug-list")
public List<UserAccount> debugList() {
    System.out.println("Debug: ユーザー一覧を取得します"); // ログに出ると安心
    return userService.findAll();
}
```

---

この場合は `UserAccountService` に以下の記述を追加する必要があります。

```java
// 全ユーザーの一覧を取得する（デバッグ用）
public List<UserAccount> findAll() {
    return repository.findAll();
}
```

#### SecurityConfigの修正

このAPIを有効にするためにも、該当箇所に次の記述を貼ってください。

```java
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**") // CSRF対策をH2Consoleだけ無効化（これがないとログインボタンが効かない）
                .disable() 
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // H2 Consoleは内部でFramesetを使っているため、その表示を許可する
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll() // H2 Consoleへのアクセスをすべての人に許可
                .requestMatchers("/api/users/register", "/api/users/login", "/api/users/debug-list", "/api/attendance/**").permitAll()
                .anyRequest().authenticated()
            );
```

---

```java
        return http.build();
    }
```

#### 動作確認の手順

1. **Spring Boot を起動** (`./mvnw spring-boot:run`)
2. **再度 `curl` でユーザー登録**（メモリDBなので再起動で消えているため）

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{
  "userId":"test-user@example.com", 
  "userName":"テスト", 
  "password":"mysecretpassword"
}' \
http://localhost:8080/api/users/register
```

---

1. **ブラウザで確認用APIを叩く**
以下のURLをブラウザに貼り付けてください。
`https://[あなたのCodespacesのURL]-8080.app.github.dev/api/users/debug-list`

#### 成功すると

ブラウザに以下のような JSON が表示されます。

```json
[
  {
    "id": 1,
    "userId": "test-user@example.com",
    "userName": "テスト",
    "password": "$2a$10$..." 
  }
]
```

---

### これで何がわかるか？

* **何も表示されない (`[]`) 場合**: `curl` での登録自体が成功していません。
* **データはあるが Vue.js でログインできない場合**: パスワードの照合ロジック（`BCrypt`）か、Vue.js から送っている JSON の形式に問題があることが特定できます。

まずはこの「デバッグ窓口」から、データが生きているか確認してみましょう。これなら 403 エラーに悩まされることなく、確実に中身がわかります！
