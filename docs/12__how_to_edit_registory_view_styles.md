---
marp: true
style: |
  section.frontpage {
    text-align: center;
  }
  section p, section li {
    font-size: 24px;
  }
  section.codepage pre {
    font-size: 18px;
  }
---
<!-- _class: frontpage -->
# `App.vue` と `RegisterForm.vue` のUI調整

---
<!-- _class: codepage -->
## 1. `App.vue` のボタン調整

まずは、ログイン画面に表示されている「新規ユーザー登録」ボタンのサイズと間隔を微調整します。

```css
<style scoped>
.switch-mode-link {
  margin-top: 30px; /* メッセージとの間隔を広げる */
  text-align: center;
}

.text-button {
  background-color: #007bff; /* ログインボタンと同色（青系） */
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 20px; /* ボタンを大きく */
  font-size: 1rem;
  cursor: pointer; /* マウスカーソルを指マークに */
  transition: background-color 0.3s;
}

.text-button:hover {
  background-color: #0056b3; /* ホバー時に少し暗く */
}
</style>
```

---
<!-- _class: codepage -->
## 2. `RegisterForm.vue` のデザイン刷新

各種ラベルの間隔を整え、ダーク背景でも見やすいリンク色を設定した構成案です。

```html
<template>
  <div class="register-container">
    <div class="register-card">
      <h2 class="form-title">ユーザー登録</h2>
      
      <form @submit.prevent="handleRegister" class="registration-form">
        <div class="form-group">
          <label for="userName">ユーザー名</label>
          <input id="userName" v-model="userName" type="text" required placeholder="例：田中 太郎" />
        </div>

        <div class="form-group">
          <label for="userId">ユーザーID (メールアドレス)</label>
          <input id="userId" v-model="userId" type="email" required placeholder="example@mail.com" />
        </div>

        <div class="form-group">
          <label for="password">パスワード</label>
          <input id="password" v-model="password" type="password" required />
        </div>
```

---

```html
        <div class="form-group">
          <label for="passwordConfirm">パスワード (確認用)</label>
          <input id="passwordConfirm" v-model="passwordConfirm" type="password" required />
        </div>

        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <div class="button-group">
          <button type="submit" class="submit-btn">これで登録する</button>
          <button type="button" @click="resetForm" class="reset-btn">リセット</button>
        </div>
      </form>

      <div class="footer-link">
        <span @click="$emit('switch-to-login')" class="login-link">
          既にアカウントをお持ちの方はこちら（ログイン画面へ）
        </span>
      </div>
    </div>
  </div>
</template>
```

---

```css
<style scoped>
/* コンテナ全体の余白 */
.register-container {
  padding-top: 40px; /* 見出し上部のスペース */
  display: flex;
  justify-content: center;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 20px;
  /* 背景が黒い環境を想定し、カード形式で浮き立たせる場合はここを調整 */
}

.form-title {
  text-align: center;
  margin-bottom: 30px;
}

/* フォームグループ（ラベルと入力欄のセット）の間隔 */
.form-group {
  margin-bottom: 20px; /* 上下の間隔を確保 */
  display: flex;
  flex-direction: column; /* ラベルを上に、入力欄を下に配置 */
  text-align: left; /* 左寄せにしてフォームらしさを強調 */
}
```

---

```css
.form-group label {
  margin-bottom: 8px;
  font-weight: bold;
}

.form-group input {
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ccc;
  font-size: 1rem;
}

.error-message {
  color: #ff4444;
  margin-bottom: 15px;
  text-align: center;
}

/* ボタンエリアの設定 */
.button-group {
  margin-top: 30px; /* フォームとの間隔 */
  display: flex;
  gap: 15px; /* ボタン同士が接触しないように間隔を設ける */
  justify-content: center;
}

.submit-btn {
  background-color: #007bff; /* App.vueのボタンと同色 */
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  flex: 1; /* ボタンの幅を揃える */
}
```

---

```css
.reset-btn {
  background-color: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  flex: 1;
}

/* 下部リンクの調整 */
.footer-link {
  margin-top: 25px;
  text-align: center;
}

.login-link {
  color: #007bff; /* 視認性を高めるため、ログインボタンと同色の青系に設定 */
  text-decoration: underline;
  cursor: pointer;
  font-size: 0.9rem;
}

.submit-btn:hover, .login-link:hover {
  filter: brightness(1.2); /* ホバー時に少し明るくして反応を示す */
}
</style>
```

---

### 修正後の確認ポイント

1. **フォームの配置**: 各項目が縦に並び、左側にラベル、その下に入力欄が来る「標準的なフォーム形式」になっています。
2. **ボタンの独立性**: `gap: 15px` を設定したことで、登録ボタンとリセットボタンがくっつかず、押しやすくなっています。
3. **視認性**: `login-link` の色を青系（`#007bff`）にしたことで、黒背景でも沈まずにクリック可能な要素であることが伝わりやすくなっています。

まずはこのスタイルを適用してみて、実際の Codespaces の画面で見栄えを確認してみてください！もし「ここをもっとこうしたい」という細かなこだわりがあれば、さらに微調整していきましょう。
