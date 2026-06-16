---
marp: true
style: |
  section.frontpage h1 {
    text-align: center;
  }
  section p, section li {
    font-size: 24px;
  }
---
<!-- _class: frontpage -->
# 予定申請機能の実装

この機能は、一般ユーザーの未来の行動予定（有給、出張、リモートワークなど）を管理し、管理者がそれを承認することで確定させるという、非常に重要なマイルストーンです。

一般側の申請、管理者側の承認/却下、一般側ダッシュボードへの表示、ログイン時の過去予定自動クリーンアップといった全体の要件を破綻なく、かつ最も効率よく安全にビルドしていくためのロードマップ（実装のロードマップ）を整理しました。

まずはどこから着手すべきか、全体の流れとともにご案内いたします。

---

## 実装ロードマップ

```text
【ステップ 1】データベース・Entity層の確立
    └ テーブル（`future_plan`）の設計
    └ Java側 Entityクラス（`FuturePlan.java`）の作成

【ステップ 2】バックエンド（リポジトリ・サービス・コントローラー）の構築
    └ ユーザー用：予定の申請・一覧取得API
    └ 管理者用：未承認予定の一覧取得・承認/却下API
    └ ログイン時の過去予定自動削除ロジック
    └ `SecurityConfig.java` へのエンドポイント追加

【ステップ 3】フロントエンド：一般ユーザー側の画面実装
    └ 「予定申請画面」の新規作成（カレンダー形式、またはフォーム形式）
    └ 「ダッシュボード画面」に「予定一覧」エリアを新設

【ステップ 4】フロントエンド：管理者側の画面実装
    └ 統括管理者コントロールパネルに「予定申請確認」タブを追加
```

---

## まず始めるべきこと：【ステップ 1】Entityの設計

最初の着手ポイントとして、予定のデータを保持するための**エンティティ（Javaクラス）とデータベースのテーブル定義**をカチッと固めましょう。

お知らせ（`Announcement`）や打刻履歴（`AttendanceRecord`）の設計思想とシンクロさせ、UUIDによるID管理や、既存の承認ステータス（0:申請中, 1:差戻し, 2:承認済み）の仕様を踏襲した設計が最適です。

以下に、ベースとなる**Entityクラスの設計案**をご提示します。

### `FuturePlan.java`

`com.appspace.backend.entity` パッケージ配下に作成することを想定しています。

```java
package com.appspace.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
```

---

```java
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "future_plan")
@Data
public class FuturePlan {

    @Id
    private String planId; // 一意の識別子（UUIDを生成して格納）

    private String accountId; // 申請したユーザーのID
    private LocalDate planDate; // 予定対象日（例: 2026-06-15）
    private String planTitle; // 予定の区分・タイトル（例: 「有給休暇」「出張」「在宅勤務」など）
    private String planDetail; // 理由や備考（例: 「私用のため」「〇〇社訪問のため」）

    /**
     * 承認ステータス
     * 0: 申請中 (未承認)
     * 1: 差戻し (却下)
     * 2: 承認済み
     */
    private Integer planStatus;
    private String adminComment; // 差戻し・却下時に管理者が入力するコメント（空欄も許容）
    private LocalDateTime createdAt; // 申請日時（自動クリーンアップの補助やログ用）
}

```

---

## 開発を進める上でのアドバイス

予定区分が自由入力であれば、ユーザーが「有給休暇」「出張」「午前半休」「午後在宅」など、会社の運用ルールやその日の状況に合わせて柔軟に記入できるため、非常に実用的な設計になります。

また、過去データをログイン時に自動で物理削除（`DELETE`）するアプローチは、データベースの肥大化を防ぎ、システムを常に軽量に保つことに繋がります。

それでは、この仕様を完全に満たすバックエンド（Java）の基本構造（Entity層とRepository層）の具体的なソースコードをご提示します。

---

### データベース用：テーブル作成 DDL (SQL)

まずはお使いのデータベース（H2 DatabaseやMySQLなど）で、新たな予定データを格納するためのテーブル `future_plan` を作成するSQLを実行してください。

既存の打刻内容編集申請やお知らせ機能の命名規則・データ型（UUID管理、キャメルケースに対応するスネークケース表記）に美しく適合させています。

```sql
CREATE TABLE future_plan (
    plan_id VARCHAR(36) NOT NULL,            -- 一意の識別子 (UUIDを格納)
    account_id VARCHAR(36) NOT NULL,         -- 申請したユーザーのアカウントID
    plan_date DATE NOT NULL,                 -- 予定の対象日 (yyyy-MM-dd)
    plan_title VARCHAR(100) NOT NULL,        -- 予定の区分・タイトル (自由入力テキスト)
    plan_detail VARCHAR(500),                -- 理由や備考・詳細 (空欄も許容)
    plan_status INT NOT NULL DEFAULT 0,      -- 承認状態 (0:申請中, 1:差戻し, 2:承認済み)
    admin_comment VARCHAR(500),              -- 管理者からの差戻理由コメント
    created_at TIMESTAMP NOT NULL,           -- 申請日時
    PRIMARY KEY (plan_id)
);

```

---

### Repository層：`FuturePlanRepository.java`

`com.appspace.backend.repository` パッケージ配下に **`FuturePlanRepository.java`** を新規作成します。

ここでは、今回の重要仕様である「① 管理者画面での承認待ち一覧取得」「② 一般ユーザー用ダッシュボードでの予定一覧取得」「③ ログイン時の過去予定の物理削除」をSpring Data JPAの機能で綺麗に、かつ最小限の記述で実現するためのクエリメソッドをあらかじめ定義しておきます。

```java
package com.appspace.backend.repository;

import com.appspace.backend.entity.FuturePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

```

---

```java
public interface FuturePlanRepository extends JpaRepository<FuturePlan, String> {

    // 1. 特定の一般ユーザーの予定を、対象日順（昇順）に並べてすべて取得
    List<FuturePlan> findByAccountIdOrderByPlanDateAsc(String accountId);

    // 2. 管理者画面用：すべてのユーザーの「申請中 (status = 0)」の予定を古い順に取得
    List<FuturePlan> findByPlanStatusOrderByPlanDateAsc(Integer planStatus);

    // 3. 【重要仕様】操作日より前の日付となった過去の予定を物理削除する（@Queryを使わない、JPA標準ルールに則った自動削除メソッド）
    // メソッド名だけで「accountId が一致、かつ planDate が指定日より前（Before）のものを削除する」という意味
    void deleteByAccountIdAndPlanDateBefore(String accountId, LocalDate today);
}

```

### `SecurityConfig.java` へのエンドポイント追加

```java
.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                            /* ここまでに追加してきたもの */
                                "/api/dashboard/**",
                                "/api/plans/**") /* ←今回新たに追加する */
                        .permitAll()
                        .anyRequest().authenticated());
```

---

## CREATE用のSQLをcurlコマンドなどで実行する方法

DDL（テーブル作成用のSQL）を `curl` コマンドを使ってデータベースへ実行する場合、直接データベース（MySQLやPostgreSQLなど）に通信するのではなく、**Spring Boot側があらかじめ用意している「H2 DatabaseのWeb管理コンソール（H2 Console）」のバックエンド機能や、自作の初期化エンドポイントへHTTPリクエストを流し込む**という形をとるのが一般的です。

特に開発環境で **H2 Database（インメモリ）** を利用している場合、コンパイル時や実行時に `curl` からSQLを流し込むための最適なアプローチと具体的なコマンドについて解説します。

いくつか方法がありますが、開発の現場で最も手軽で間違いのない「H2 Consoleの内部機能を模倣してcurlからSQLを直接流し込む方法」をメインにご案内します。

### カレントディレクトリはどこにするべきか？

SQLの内容を直接コマンド内にテキストとして記述（インライン記述）する場合は、**カレントディレクトリはどこであっても問題ありません。**（ターミナルやコマンドプロンプトを開いた直後の場所でそのまま実行可能です）

---

もし、SQLの量が多くなったため `create_table.sql` という名前でファイルに保存し、そのファイルを読み込ませて実行したい場合は、「そのSQLファイルが保存されているフォルダ」をカレントディレクトリにしてください。

### 実行する具体的な `curl` コマンド

Spring Boot標準の H2 Console（初期状態では `http://localhost:8080/h2-console`）の通信仕様に基づいたコマンドです。

#### パターンA：コマンドにSQLを直接書いて実行する場合（カレント任意）

もっとも手軽です。以下のコマンドをそのままターミナルに貼り付けて実行してください。

```bash
curl -X POST "http://localhost:8080/h2-console/query.do" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "sql=
    CREATE TABLE future_plan (
      plan_id VARCHAR(36) NOT NULL, 
      account_id VARCHAR(36) NOT NULL, 
```

---

```bash
      plan_date DATE NOT NULL, 
      plan_title VARCHAR(100) NOT NULL, 
      plan_detail VARCHAR(500), 
      plan_status INT NOT NULL DEFAULT 0, 
      admin_comment VARCHAR(500), 
      created_at TIMESTAMP NOT NULL, 
      PRIMARY KEY (plan_id)
    );"
```

*(※WindowsのCommand Promptで実行する場合は、行末のバックスラッシュ `\` を削除して1行に繋げて実行してください)*

#### パターンB：SQLファイルから読み込んで実行する場合（ファイルがある場所がカレント）

あらかじめ `future_plan.sql` という名前でSQLを保存している場合、以下のコマンドでファイルを指定して流し込めます。

---

```bash
curl -X POST "http://localhost:8080/h2-console/query.do" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "sql@future_plan.sql"
```

### H2 Console経由で実行する場合の重要な注意点

1. **Spring Bootが起動中であること**
当然ながら、アプリケーションが起動（ポート `8080` がオープン）していないとHTTP通信を拒否されます。
2. **`application.properties` の設定**
H2のコンソール機能が有効化されており、かつ外部ツールからのリクエストを受け付ける設定（非ログイン状態でのクエリ実行）が許可されている必要があります。

```properties
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true
```

---

### 【おすすめの代替案】もっとスマートにテーブルを自動作成させる方法

もし「毎回手動で `curl` を叩いてテーブルを作るのが手間に感じる」という場合は、Spring Bootの仕組み（**自動DDL実行**）に乗っかるのが一番確実で実務的です。

#### `src/main/resources/schema.sql` に配置するだけ

Spring Bootは、起動時に `src/main/resources/` の直下に **`schema.sql`** という名前のファイルが存在すると、**内部で自動的にその中身のSQLを読み込んでテーブルを作成してくれる**強力な機能を持っています。

1. プロジェクトの `src/main/resources/` フォルダの中に、新規ファイル `schema.sql` を作成する。
2. 先ほどの `CREATE TABLE future_plan (...);` を書き込んで保存する。
3. `application.properties` に以下の一行があることを確認する。

```properties
spring.sql.init.mode=always
```

これだけで、**Spring Bootを再起動（Run）するたびに、人間が何もコマンドを叩かなくても自動的にデータベースにテーブルが用意されるようになります！**

---

### ビルド後

一つ目のターミナルで `mvnw clean spring-boot:run` を実行して、コンパイル・ビルドエラーが出ないこと、及び二つ目のターミナルで `npm run dev` を実行してログイン画面が立ち上がることを確認します。

成功したら、手動でコマンドを毎回叩く必要がなく、Spring Bootの起動時に自動で安全にテーブルが構築される仕組みがこれで完成したことになります。

## 【ステップ 2】バックエンドのService・Controller層の実装

ここでは、以下のロジックを綺麗に構築していきます。

1. **一般ユーザー用**: 未来の予定の新規申請、および自身の予定一覧の取得。
2. **ログイン時（重要仕様）**: ログインした瞬間に、操作日より前の日付となった過去の予定を自動で安全に物理削除（JPAクエリの呼び出し）する仕組み。
3. **管理者用**: 申請中の予定一覧の取得、およびそれに対する承認・却下（差戻し）アクション。

---

### Service層：`FuturePlanService.java` の新規作成

`com.appspace.backend.service` パッケージ配下に新規作成してください。
UUIDの自動払い出し、ステータスの初期化、過去予定の自動クリーンアップ処理をここに集約します。

```java
package com.appspace.backend.service;

import com.appspace.backend.entity.FuturePlan;
import com.appspace.backend.repository.FuturePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FuturePlanService {

```

---

```java
    @Autowired
    private FuturePlanRepository futurePlanRepository;

    /**
     * ロジック1: 一般ユーザーからの新しい予定申請をデータベースに登録する
     */
    public void createFuturePlan(String accountId, LocalDate planDate, String planTitle, String planDetail) {
        FuturePlan plan = new FuturePlan();
        plan.setPlanId(UUID.randomUUID().toString()); // 新しいUUIDを発行
        plan.setAccountId(accountId);
        plan.setPlanDate(planDate);
        plan.setPlanTitle(planTitle.trim());
        plan.setPlanDetail(planDetail != null ? planDetail.trim() : "");
        plan.setPlanStatus(0); // 0: 申請中 (未承認) で初期化
        plan.setAdminComment("");
        plan.setCreatedAt(LocalDateTime.now());

        futurePlanRepository.save(plan);
        System.out.println("=== [Service] 予定申請を登録しました (ID: " + plan.getPlanId() + ") ===");
    }

    /**
     * ロジック2: 特定の一般ユーザーの予定一覧を取得する
     */
    public List<FuturePlan> getPlansByAccount(String accountId) {
        return futurePlanRepository.findByAccountIdOrderByPlanDateAsc(accountId);
    }

    /**
     * ロジック3: 管理者画面用：すべての「申請中 (0)」の予定を一覧取得する
     */
    public List<FuturePlan> getPendingPlansForAdmin() {
        return futurePlanRepository.findByPlanStatusOrderByPlanDateAsc(0);
    }

```

---

```java
    /**
     * ロジック4: 管理者による承認、または却下（差戻し）の裁定を反映する
     */
    public void judgePlan(String planId, Integer targetStatus, String adminComment) {
        FuturePlan plan = futurePlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("指定された予定申請が見つかりませんでした。ID: " + planId));

        // ステータスを更新 (1:差戻し, 2:承認済み)
        plan.setPlanStatus(targetStatus);
        plan.setAdminComment(adminComment != null ? adminComment.trim() : "");

        futurePlanRepository.save(plan);
        System.out.println("=== [Service] 予定の判定を更新しました (ID: " + planId + ", Status: " + targetStatus + ") ===");
    }

    /**
     * 【重要仕様】ログイン時用：操作日より過去の日付となった予定を自動で物理削除する
     */
    @Transactional
    public void cleanPastPlans(String accountId) {
        LocalDate today = LocalDate.now();

        // Repositoryで定義した独自クエリの呼び出し
        futurePlanRepository.deleteByAccountIdAndPlanDateBefore(accountId, today);
        logger.info("=== [Service] アカウントID: {} の本日の操作日 ({}) より古い過去の予定を自動削除しました ===", accountId, today);
    }
}

```

---

### Controller層：`FuturePlanController.java` の新規作成

`com.appspace.backend.controller` パッケージ配下に新規作成してください。
一般ユーザー用と管理者用（`/api/admin/...`）の双方のエンドポイントをここに同居させます。

```java
package com.appspace.backend.controller;

import com.appspace.backend.entity.FuturePlan;
import com.appspace.backend.service.FuturePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FuturePlanController {

```

---

```java
    @Autowired
    private FuturePlanService futurePlanService;

    // =================================================================
    // 一般ユーザー向けエンドポイント群
    // =================================================================

    /**
     * 窓口1: 一般ユーザーからの新規予定申請の受付
     * POST http://localhost:8080/api/plans/request
     */
    @PostMapping("/plans/request")
    public ResponseEntity<String> requestFuturePlan(@RequestBody Map<String, Object> payload) {
        String accountId = (String) payload.get("accountId");
        String planDateStr = (String) payload.get("planDate"); // "yyyy-MM-dd"
        String planTitle = (String) payload.get("planTitle");
        String planDetail = (String) payload.get("planDetail");

        // 二重防壁バリデーション
        if (accountId == null || accountId.trim().isEmpty()) return ResponseEntity.badRequest().body("【却下】アカウントIDが不正です。");
        if (planDateStr == null || planDateStr.trim().isEmpty()) return ResponseEntity.badRequest().body("【却下】予定対象日を指定してください。");
        if (planTitle == null || planTitle.trim().isEmpty()) return ResponseEntity.badRequest().body("【却下】予定の区分・タイトルを入力してください。");

        try {
            LocalDate planDate = LocalDate.parse(planDateStr);
            // 安全策: 過去日付への申請は弾く
            if (planDate.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body("【却下】過去の日付に対して新しく予定を申請することはできません。");
            }

            futurePlanService.createFuturePlan(accountId, planDate, planTitle, planDetail);
            return ResponseEntity.ok("予定申請を正常に提出しました。管理者の承認をお待ちください。");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("予定申請の処理中にエラーが発生しました: " + e.getMessage());
        }
    }

```

---

```java
    /**
     * 窓口2: 一般ユーザー用：自身のダッシュボードに表示する「予定一覧」の取得
     * GET http://localhost:8080/api/plans/my-list?accountId=xxx
     */
    @GetMapping("/plans/my-list")
    public ResponseEntity<?> getMyPlans(@RequestParam String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("アカウントIDが指定されていません。");
        }
        try {
            List<FuturePlan> myList = futurePlanService.getPlansByAccount(accountId);
            return ResponseEntity.ok(myList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("予定一覧の取得に失敗しました。");
        }
    }

    /**
     * 【重要仕様】窓口3: ログイン時呼び出し専用：過去の予定を自動削除するトリガー
     * POST http://localhost:8080/api/plans/sync-login
     */
    @PostMapping("/plans/sync-login")
    public ResponseEntity<String> syncLoginCleanPastPlans(@RequestBody Map<String, String> payload) {
        String accountId = payload.get("accountId");
        if (accountId == null || accountId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("同期エラー: アカウントIDが不明です。");
        }
        try {
            // 過去の予定データを物理消去
            futurePlanService.cleanPastPlans(accountId);
            return ResponseEntity.ok("過去日の予定データのリフレッシュに成功しました。");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("ログイン時の予定データ自動クリーンアップ中にエラーが発生しました。");
        }
    }

```

---

```java
    // =================================================================
    // 管理者ユーザー（Admin）向けエンドポイント群
    // =================================================================

    /**
     * 窓口4: 管理者用：現在届いている「未承認の予定申請」をすべて一覧取得する
     * GET http://localhost:8080/api/admin/plans/pending-list
     */
    @GetMapping("/admin/plans/pending-list")
    public ResponseEntity<?> getAdminPendingPlans() {
        try {
            List<FuturePlan> pendingList = futurePlanService.getPendingPlansForAdmin();
            return ResponseEntity.ok(pendingList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("未承認申請リストの取得に失敗しました。");
        }
    }

    /**
     * 窓口5: 管理者用：届いた申請を「承認」または「差戻し」する
     * POST http://localhost:8080/api/admin/plans/judge
     */
    @PostMapping("/admin/plans/judge")
    public ResponseEntity<String> judgeUserPlan(@RequestBody Map<String, Object> payload) {
        String planId = (String) payload.get("planId");
        Integer targetStatus = (Integer) payload.get("targetStatus"); // 1:差戻し, 2:承認
        String adminComment = (String) payload.get("adminComment");

        if (planId == null || planId.trim().isEmpty()) return ResponseEntity.badRequest().body("【却下】対象の申請IDが指定されていません。");
        if (targetStatus == null || (targetStatus != 1 && targetStatus != 2)) return ResponseEntity.badRequest().body("【却下】判定ステータスが不正です。");

        try {
            futurePlanService.judgePlan(planId, targetStatus, adminComment);
            String message = (targetStatus == 2) ? "予定申請を「承認」しました。" : "予定申請を「差戻し」しました。";
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("判定処理中にエラーが発生しました: " + e.getMessage());
        }
    }
}

```

---

### ロジックのポイント解説

* **物理削除エンドポイント（`/plans/sync-login`）の設計意図**
フロントエンド側のログインコンポーネント（`LoginForm.vue`）で認証が成功し、`localStorage.setItem('user', ...)` が行われた直後に、このURLに対してアカウントIDをのせて非同期リクエストを1発パッと送信するようにします。
これにより、「ログイン時に自動的に過去データが削除される」という挙動を、バックエンドのトランザクションの安全性を損なわずに実現できます。
* **安全な未来申請ガード**
一般ユーザーからの登録時、対象日が今日より前（`isBefore(LocalDate.now())`）である場合は「過去への予定登録は不可」としてバックエンド側で強固にブロックします。

---

## 【ステップ3】フロントエンド：一般ユーザー側の画面実装

今回は以下の3つのパートに分けて、ソースコードの作成・組み込みを行います。

1. **`LoginView.vue`（またはログイン処理を行っている箇所）**: ログイン成功の瞬間に、過去予定を自動消去するAPI（`/api/plans/sync-login`）をトリガーする処理の追加
2. **`FuturePlanView.vue`（新規作成）**: 一般ユーザーが予定を入力・提出するための「予定申請画面」
3. **`DashboardView.vue`（既存改修）**: 申請中・承認済みの予定がある場合に自動で出現する「予定一覧」エリアの新設

### パート1：ログイン時の自動削除連動 ( `LoginForm.vue` )

ユーザーがログインに成功し、`localStorage` にユーザー情報を保存した直後の位置に、バックエンドの自動削除APIを叩く処理（`syncLoginPastPlans`）を挟み込みます。

---

#### スクリプト側に追加する記述のイメージ

ログインボタンのクリックイベント内に、以下のような関数を呼び出すように追記してください。

```javascript
// --- ログイン成功時の処理ブロック内 ---
// localStorage.setItem('user', JSON.stringify(response.data)); などの直後

const syncLoginPastPlans = async (accountId) => {
  try {
    // バックエンドのログイン時クリーンアップAPIを呼び出す
    await apiClient.post('/plans/sync-login', { accountId: accountId });
    console.log('=== [Login Sync] 過去の予定データの自動リフレッシュが完了しました ===');
  } catch (error) {
    console.error('ログイン時の予定同期エラー:', error);
  }
};
```

---

```javascript
// ログイン成功時にアカウントIDを渡して実行
if (response.data.accountId) {
    await syncLoginPastPlans(response.data.accountId);
}
```

### パート2：新規作成 `FuturePlanView.vue` (予定申請画面)

一般ユーザーが予定を新規申請するための画面です。`src/views/` 配下に **`FuturePlanView.vue`** という名前で新しくファイルを作成し、以下のコードをそのまま貼り付けてください。

```html
<template>
  <div class="future-plan-container">
    <h2>予定申請フォーム</h2>
    <p class="subtitle">有給休暇、在宅勤務、出張などの未来の予定を申請できます。</p>

    <div class="plan-form-card">
      <div class="form-group">
        <label>予定対象日 <span class="required">※必須</span></label>
```

---

```html
        <input v-model="form.planDate" type="date" :min="todayStr" />
      </div>

      <div class="form-group">
        <label>予定の区分・タイトル <span class="required">※必須</span></label>
        <input 
          v-model="form.planTitle" 
          type="text" 
          placeholder="例: 有給休暇、出張、在宅勤務、午前半休 など"
          maxlength="50"
        />
      </div>

      <div class="form-group">
        <label>理由・備考詳細</label>
        <textarea 
          v-model="form.planDetail" 
          placeholder="例: 私用のため。〇〇社訪問のため。" 
          rows="4"
          maxlength="200"
        ></textarea>
      </div>

```

---

```html
      <div class="form-actions">
        <button @click="submitPlan" class="btn-submit">予定申請を提出する</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import apiClient from '../api';

const todayStr = ref('');
const loggedInAccountId = ref('');

const form = ref({
  planDate: '',
  planTitle: '',
  planDetail: ''
});

// 初期化（今日の日付より前の過去日を選べないように最小値を設定）
const initForm = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  todayStr.value = `${y}-${m}-${d}`;
  form.value.planDate = todayStr.value; // 初期値として今日をセット
};

```

---

```javascript
const submitPlan = async () => {
  if (!form.value.planDate) { alert('予定対象日を指定してください。'); return; }
  if (!form.value.planTitle.trim()) { alert('予定の区分・タイトルを入力してください。'); return; }

  if (!confirm('この内容で未来の予定申請を提出してもよろしいですか？')) {
    return;
  }

  try {
    await apiClient.post('/plans/request', {
      accountId: loggedInAccountId.value,
      planDate: form.value.planDate,
      planTitle: form.value.planTitle,
      planDetail: form.value.planDetail
    });

    alert('予定申請を提出しました！管理者の承認をお待ちください。');
    
    // フォームをリセット
    form.value.planTitle = '';
    form.value.planDetail = '';
  } catch (error) {
    console.error('予定申請エラー:', error);
    alert(error.response?.data || '予定申請の提出に失敗しました。');
  }
};

onMounted(() => {
  initForm();
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId;
  }
});
</script>

```

---

```css
<style scoped>
.future-plan-container { max-width: 600px; margin: 40px auto; padding: 0 20px; font-family: sans-serif; }
.subtitle { color: #666; margin-bottom: 25px; }
.plan-form-card { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.form-group { margin-bottom: 20px; text-align: left; }
.form-group label { display: block; margin-bottom: 6px; font-weight: bold; color: #444; font-size: 0.95rem; }
.form-group input[type="date"], .form-group input[type="text"], .form-group textarea {
  width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; font-size: 1rem;
}
.required { color: #d32f2f; }
.form-actions { margin-top: 30px; }
.btn-submit {
  width: 100%; background-color: #007bff; color: white; border: none; padding: 12px; border-radius: 4px;
  font-size: 1.05rem; font-weight: bold; cursor: pointer; transition: background-color 0.2s;
}
.btn-submit:hover { background-color: #0056b3; }
</style>

```

### パート3：既存改修 `DashboardView.vue` (ダッシュボードの予定一覧エリア)

一般ユーザーのメイン画面（ダッシュボード）の下部などに、「申請した予定や確定した予定がある場合のみ自動で出現する予定一覧エリア」を追加します。

---

#### ① `<template>` への追加コード

```html
<div v-if="myPlans && myPlans.length > 0" class="my-plans-section">
      <h3>あなたの予定一覧</h3>
      <p class="section-desc">申請中、または確定している未来の予定事項です。</p>
      
      <div class="plans-grid">
        <div v-for="plan in myPlans" :key="plan.planId" class="plan-item-card">
          <div class="plan-card-header">
            <span class="plan-date-badge">{{ formatPlanDate(plan.planDate) }}</span>
            <span class="status-badge" :class="getPlanStatusClass(plan.planStatus)">
              {{ getPlanStatusLabel(plan.planStatus) }}
            </span>
          </div>
          
          <div class="plan-card-body">
            <h4>{{ plan.planTitle }}</h4>
            <p v-if="plan.planDetail" class="plan-detail-text">{{ plan.planDetail }}</p>
            <div v-if="plan.planStatus === 1 && plan.adminComment" class="admin-comment-alert">
              <strong>差戻理由:</strong> {{ plan.adminComment }}
            </div>
          </div>
        </div>
      </div>
    </div>
```

---

#### ② `<script setup>` への追加コード

バックエンドの `GET /api/plans/my-list` からデータを引っ張ってくるリアクティブ変数と通信関数を追加します。

```javascript
// --- DashboardView.vue の script setup 内へ追加 ---
import { ref, onMounted } from 'vue';
import apiClient from '../api'; // 既存の共通APIクライアント

const myPlans = ref([]);
const loggedInAccountId = ref('');

// 自分の予定一覧をバックエンドからロードする
const fetchMyPlans = async () => {
  if (!loggedInAccountId.value) return;
  try {
    const response = await apiClient.get('/plans/my-list', {
      params: { accountId: loggedInAccountId.value }
    });
    myPlans.value = response.data;
```

---

```javascript
  } catch (error) {
    console.error('予定一覧の取得に失敗しました:', error);
  }
};

// 日付の表示調整 (例: "2026-06-15" -> "6月15日")
const formatPlanDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

// ステータス名変換
const getPlanStatusLabel = (status) => {
  if (status === 0) return '申請中';
  if (status === 1) return '差戻し';
  if (status === 2) return '承認済み';
  return '不明';
};

// ステータスに応じたクラス分け
const getPlanStatusClass = (status) => {
  if (status === 0) return 'status-pending';
  if (status === 1) return 'status-rejected';
  if (status === 2) return 'status-approved';
  return '';
};

// 既存の onMounted 内に以下のロード処理を追記・マージします
onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId;
    
    // 予定一覧をロード
    fetchMyPlans();
  }
});
```

---

#### ③ `<style scoped>` への追加コード

予定一覧をカード形式で並べるためのグリッドデザインと、ステータスバッジのスタイルです。

```css
/* --- DashboardView.vue の style scoped の末尾に追加 --- */

.my-plans-section { margin-top: 40px; text-align: left; }
.section-desc { color: #666; font-size: 0.9rem; margin-bottom: 15px; }
.plans-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }

.plan-item-card {
  background: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.04); display: flex; flex-direction: column; justify-content: space-between;
}
.plan-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.plan-date-badge { background-color: #f0f4f8; color: #102a43; font-weight: bold; padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; }

.status-badge { font-size: 0.8rem; font-weight: bold; padding: 3px 8px; border-radius: 12px; }
.status-pending { background-color: #fff3e0; color: #f57c00; }
.status-rejected { background-color: #ffebee; color: #d32f2f; }
.status-approved { background-color: #e8f5e9; color: #388e3c; }

.plan-card-body h4 { margin: 0 0 6px 0; font-size: 1.1rem; color: #333; }
.plan-detail-text { margin: 0; font-size: 0.9rem; color: #666; word-break: break-all; }
.admin-comment-alert {
    margin-top: 10px;
    padding: 6px;
    background-color: #fff8f0;
    border-left: 3px solid #f57c00;
    font-size: 0.85rem;
    border-radius: 4px;
}

```

---

### 画面側の解説と動作の見通し

* **`v-if="myPlans && myPlans.length > 0"` の安心感**
ご要望の通り、予定データが1件もない場合はダッシュボード上に「予定一覧」の枠組み自体が非表示（マウントされない）になります。そのため、普段の画面をスッキリと美しく保つことができます。
* **未来日制限のUX（`min="todayStr"`）**
`FuturePlanView.vue` の日付入力フォームに `:min="todayStr"` を仕込んだことで、カレンダーピッカー上で「今日より前の過去日」がグレーアウトして選択できなくなります。バックエンド側の防壁と合わせて二重の安全対策となっています。

これで一般ユーザー側の申請・表示環境のパーツが揃いました！
Vueファイルを保存・反映させ、予定を登録してみてください。ダッシュボードに「申請中」のカードがパッと出現すれば大成功です。

---

### パート4：管理者画面への「予定申請確認」タブの実装

* 全ユーザーから届いた未承認の予定申請（`planStatus === 0`）をカード型でズラリと一覧表示し、その場で「承認」「差戻し（コメント付き）」を裁定できる管理パネルを構築します。

  * **【追加改修】一般ユーザー用予定申請画面（`FuturePlanView.vue`）への「自身の申請履歴一覧（検索機能付きテーブル）」の搭載**

* 「申請フォームの上のエリア」に、自分が過去に申請したすべての予定（承認済み・差戻し含む）をステータス別で絞り込める、実務クオリティの検索機能付きテーブルを新設します。

まずは、管理者側の新コンポーネントの作成から進めます。

---

#### 1. 管理者側：`AdminFuturePlanPanel.vue` の新規作成

管理者が未承認の予定をパッと確認・裁定するための専用パネルです。打刻修正承認のUXを踏襲し、差戻し時のみコメント入力を求める親切設計にしています。

`src/components/`（または管理者用コンポーネントのディレクトリ）に **`AdminFuturePlanPanel.vue`** を新規作成し、以下のコードを記述してください。

```html
<template>
  <div class="admin-plan-panel">
    <div class="panel-header">
      <h3>ユーザー 予定申請承認パネル</h3>
      <p class="panel-subtitle">全ユーザーから提出された「未来の行動予定」の確認と承認・差戻し処理が行えます。</p>
    </div>

    <div v-if="pendingPlans.length === 0" class="no-data-alert">
      現在、未処理の予定申請はありません。
    </div>
    <div v-else class="plans-grid">
      <div v-for="plan in pendingPlans" :key="plan.planId" class="plan-card">
        <div class="card-header">
          <span class="user-id">社員ID: {{ plan.accountId }}</span>
          <span class="target-date">対象日: {{ formatPlanDate(plan.planDate) }}</span>
        </div>
```

---

```html

        <div class="card-body">
          <div class="plan-title-area">
            <span class="title-label">予定区分:</span>
            <span class="title-value">{{ plan.planTitle }}</span>
          </div>

          <div v-if="plan.planDetail" class="plan-detail-area">
            <strong>理由・詳細備考:</strong>
            <p class="detail-text">{{ plan.planDetail }}</p>
          </div>

          <div v-if="activeRejectId === plan.planId" class="reject-comment-area">
            <label>差戻し理由を入力してください <span class="required">※必須</span></label>
            <textarea 
              v-model="adminComment" 
              placeholder="例: この日は全体会議があるため、別日への変更をお願いします。" 
              rows="2"
            ></textarea>
          </div>
        </div>

```

---

```html
        <div class="card-actions">
          <template v-if="activeRejectId !== plan.planId">
            <button @click="handleJudge(plan.planId, 2)" class="btn-approve">承認する</button>
            <button @click="showRejectInput(plan.planId)" class="btn-trigger-reject">❌ 差戻す</button>
          </template>
          <template v-else>
            <button @click="handleJudge(plan.planId, 1)" class="btn-reject-confirm">この理由で差戻しを確定</button>
            <button @click="cancelReject" class="btn-cancel">キャンセル</button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import apiClient from '../api';

const pendingPlans = ref([]);
const activeRejectId = ref(null);
const adminComment = ref('');

// 1. 未承認の予定一覧を取得
const fetchPendingPlans = async () => {
  try {
    const response = await apiClient.get('/admin/plans/pending-list');
    pendingPlans.value = response.data;
  } catch (error) {
    console.error('予定申請リストの取得に失敗しました:', error);
  }
};

```

---

```javascript
const showRejectInput = (planId) => {
  activeRejectId.value = planId;
  adminComment.value = '';
};

const cancelReject = () => {
  activeRejectId.value = null;
  adminComment.value = '';
};

// 2. 承認（2）または差戻し（1）の実行
const handleJudge = async (planId, targetStatus) => {
  if (targetStatus == 1 && !adminComment.value.trim()) {
    alert('ユーザーへ伝える「差戻し理由」を入力してください。');
    return;
  }

  const confirmMsg = targetStatus === 2 ? 'この予定申請を承認しますか？' : 'この予定申請を差し戻しますか？';
  if (!confirm(confirmMsg)) return;

  try {
    const response = await apiClient.post('/admin/plans/judge', {
      planId: planId,
      targetStatus: targetStatus,
      adminComment: targetStatus === 1 ? adminComment.value : ''
    });
    alert(response.data);
    cancelReject();
    fetchPendingPlans(); // 最新状態にリフレッシュ
  } catch (error) {
    console.error('判定処理エラー:', error);
    alert('処理に失敗しました。');
  }
};

```

---

```javascript
const formatPlanDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

onMounted(() => {
  fetchPendingPlans();
});
</script>

<style scoped>
.admin-plan-panel { padding: 5px; text-align: left; font-family: sans-serif; }
.panel-header { margin-bottom: 20px; border-bottom: 1px solid #eceff1; padding-bottom: 10px; }
.panel-header h3 { margin: 0; color: #2c3e50; font-size: 1.2rem; }
.panel-subtitle { color: #666; font-size: 0.9rem; margin: 4px 0 0 0; }

.no-data-alert { background-color: #e8f5e9; color: #2e7d32; padding: 20px; border-radius: 6px; text-align: center; font-weight: bold; border: 1px solid #c8e6c9; }

.plans-grid { display: flex; flex-direction: column; gap: 20px; }
.plan-card { background: white; border-radius: 8px; border: 1px solid #cfd8dc; box-shadow: 0 4px 10px rgba(0,0,0,0.05); overflow: hidden; }

.card-header { background-color: #455a64; color: white; padding: 12px 20px; display: flex; justify-content: space-between; font-weight: bold; font-size: 0.95rem; }
.card-body { padding: 20px; }

.plan-title-area { font-size: 1.1rem; font-weight: bold; color: #102a43; margin-bottom: 12px; }
.title-label { color: #627d98; margin-right: 8px; font-size: 0.95rem; }

.plan-detail-area { background: #f8f9fa; border-left: 4px solid #b0bec5; padding: 10px 15px; border-radius: 4px; }
.plan-detail-area strong { font-size: 0.85rem; color: #486581; }
.detail-text { margin: 4px 0 0 0; font-size: 0.95rem; color: #334e68; line-height: 1.4; }

.reject-comment-area { margin-top: 15px; background: #fff3e0; padding: 12px; border-radius: 4px; border: 1px solid #ffe0b2; }
.reject-comment-area label { display: block; font-weight: bold; font-size: 0.85rem; color: #e65100; margin-bottom: 6px; }
.reject-comment-area textarea { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
.required { color: #d32f2f; }

.card-actions { background: #f5f7f8; padding: 12px 20px; display: flex; gap: 12px; justify-content: flex-end; border-top: 1px solid #eceff1; }
.btn-approve { background-color: #4caf50; color: white; border: none; padding: 8px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-approve:hover { background-color: #388e3c; }
.btn-trigger-reject { background-color: white; color: #d32f2f; border: 1px solid #d32f2f; padding: 8px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-trigger-reject:hover { background-color: #ffebee; }
.btn-reject-confirm { background-color: #e65100; color: white; border: none; padding: 8px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-cancel { background-color: #cfd8dc; color: #37474f; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
</style>

```

---

#### ① 統括管理者コントロールパネル親画面へのタブ追加

既存の管理者用コントロールパネルのメインファイル（「お知らせ管理」タブを末尾に追加したファイル）を開き、新設したコンポーネントを3番目（打刻モード一括制御の手前）に滑り込ませます。

```html
<button @click="currentTab = 'adminPlans'" :class="{ active: currentTab === 'adminPlans' }">
  予定申請確認
</button>

<div v-if="currentTab === 'adminPlans'">
  <AdminFuturePlanPanel />
</div>

import AdminFuturePlanPanel from './AdminFuturePlanPanel.vue';
```

---

### 2. 一般ユーザー側：検索機能付きテーブル搭載の `FuturePlanView.vue`

続いて、一般ユーザーが「自分の出した申請状況（承認・差戻し・申請中）」を、**検索・ステータス絞り込み付きのテーブル形式**でフォームの上部でいつでも見渡せるよう、`FuturePlanView.vue` を一気に超豪華仕様へアップデートします。

既存の **`FuturePlanView.vue`** を以下のコードで丸ごと上書きしてください。

```html
<template>
  <div class="future-plan-container">
    <h2>行動予定申請・管理</h2>
    <p class="subtitle">未来の行動予定の申請と、提出した申請の状況確認が行えます。</p>

    <div class="history-section-box">
      <div class="history-header">
        <h3>あなたの申請履歴一覧</h3>
        
        <div class="filter-controls">
          <label for="statusFilter">状況で絞り込み:</label>
          <select id="statusFilter" v-model="filterStatus" class="select-filter">
```

---

```html
            <option value="all">すべて表示</option>
            <option value="0">申請中</option>
            <option value="2">承認済み</option>
            <option value="1">差戻し</option>
          </select>
        </div>
      </div>

      <div v-if="filteredPlans.length > 0" class="table-wrapper">
        <table class="history-table">
          <thead>
            <tr>
              <th>予定対象日</th>
              <th>予定の区分</th>
              <th>理由・備考詳細</th>
              <th>承認状況</th>
              <th>管理者からの伝言</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="plan in filteredPlans" :key="plan.planId">
              <td class="date-td">{{ formatPlanDate(plan.planDate) }}</td>
              <td class="title-td">{{ plan.planTitle }}</td>
              <td class="detail-td">{{ plan.planDetail || '-' }}</td>
              <td>
                <span class="status-badge" :class="getPlanStatusClass(plan.planStatus)">
                  {{ getPlanStatusLabel(plan.planStatus) }}
                </span>
              </td>
              <td class="comment-td" :class="{ 'has-comment': plan.adminComment }">
                {{ plan.adminComment || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
```

---

```html
      <div v-else class="empty-table-alert">
        該当する予定申請データは見つかりませんでした。
      </div>
    </div>

    <div class="plan-form-card">
      <h3>新規予定申請フォーム</h3>
      <div class="form-group">
        <label>予定対象日 <span class="required">※必須</span></label>
        <input v-model="form.planDate" type="date" :min="todayStr" />
      </div>

      <div class="form-group">
        <label>予定の区分・タイトル <span class="required">※必須</span></label>
        <input 
          v-model="form.planTitle" 
          type="text" 
          placeholder="例: 有給休暇、出張、在宅勤務、午前半休 など"
          maxlength="50"
        />
      </div>

      <div class="form-group">
        <label>理由・備考詳細</label>
        <textarea 
          v-model="form.planDetail" 
          placeholder="例: 私用のため。〇〇社訪問のため。" 
          rows="3"
          maxlength="200"
        ></textarea>
      </div>

      <div class="form-actions">
        <button @click="submitPlan" class="btn-submit">予定申請を提出する</button>
      </div>
    </div>
  </div>
</template>

```

---

```javascript
<script setup>
import { ref, onMounted, computed } from 'vue';
import apiClient from '../api';

const todayStr = ref('');
const loggedInAccountId = ref('');
const myPlans = ref([]);      // サーバーから取得した全履歴
const filterStatus = ref('all'); // 絞り込み条件（'all', '0', '1', '2'）

const form = ref({
  planDate: '',
  planTitle: '',
  planDetail: ''
});

// 自分の全予定履歴をロードする関数
const fetchMyPlans = async () => {
  if (!loggedInAccountId.value) return;
  try {
    const response = await apiClient.get('/plans/my-list', {
      params: { accountId: loggedInAccountId.value }
    });
    myPlans.value = response.data;
  } catch (error) {
    console.error('履歴の取得に失敗しました:', error);
  }
};

// リアルタイム絞り込み検索ロジック (computed)
const filteredPlans = computed(() => {
  if (filterStatus.value === 'all') {
    return myPlans.value;
  }
  return myPlans.value.filter(plan => plan.planStatus === Number(filterStatus.value));
});

```

---

```javascript
const initForm = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  todayStr.value = `${y}-${m}-${d}`;
  form.value.planDate = todayStr.value;
};

const submitPlan = async () => {
  if (!form.value.planDate) { alert('予定対象日を指定してください。'); return; }
  if (!form.value.planTitle.trim()) { alert('予定の区分・タイトルを入力してください。'); return; }

  if (!confirm('この内容で未来の予定申請を提出してもよろしいですか？')) return;

  try {
    await apiClient.post('/plans/request', {
      accountId: loggedInAccountId.value,
      planDate: form.value.planDate,
      planTitle: form.value.planTitle,
      planDetail: form.value.planDetail
    });

    alert('予定申請を提出しました！');
    form.value.planTitle = '';
    form.value.planDetail = '';
    fetchMyPlans(); // 提出成功後に一覧を自動再ロード！
  } catch (error) {
    console.error('予定申請エラー:', error);
    alert(error.response?.data || '予定申請の提出に失敗しました。');
  }
};

const formatPlanDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

const getPlanStatusLabel = (status) => {
  if (status === 0) return '申請中';
  if (status === 1) return '差戻し';
  if (status === 2) return '承認済み';
  return '不明';
};

const getPlanStatusClass = (status) => {
  if (status === 0) return 'status-pending';
  if (status === 1) return 'status-rejected';
  if (status === 2) return 'status-approved';
  return '';
};

```

---

```javascript
onMounted(() => {
  initForm();
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId;
    fetchMyPlans(); // 画面展開時に履歴を引く
  }
});
</script>

<style scoped>
.future-plan-container { max-width: 850px; margin: 40px auto; padding: 0 20px; font-family: sans-serif; text-align: left; }
.subtitle { color: #666; margin-bottom: 30px; }

/* 履歴セクションのデザイン */
.history-section-box {
  background: white;
  border-radius: 8px;
  border: 1px solid #cfd8dc;
  padding: 20px;
  margin-bottom: 35px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.04);
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #eceff1;
  padding-bottom: 12px;
  margin-bottom: 15px;
}
.history-header h3 { margin: 0; color: #37474f; font-size: 1.15rem; }

```

---

```css
.filter-controls { display: flex; align-items: center; gap: 8px; font-size: 0.9rem; font-weight: bold; color: #546e7a; }
.select-filter {
  padding: 6px 12px;
  border: 1px solid #b0bec5;
  border-radius: 4px;
  font-size: 0.9rem;
  background: #fafafa;
  cursor: pointer;
}

/* 検索機能付きテーブルのデザイン */
.table-wrapper { overflow-x: auto; }
.history-table { width: 100%; border-collapse: collapse; font-size: 0.95rem; text-align: left; }
.history-table th { background-color: #f5f7f8; color: #37474f; padding: 12px 10px; font-weight: bold; border-bottom: 2px solid #cfd8dc; }
.history-table td { padding: 12px 10px; border-bottom: 1px solid #eceff1; vertical-align: middle; }

.date-td { font-weight: bold; color: #263238; }
.title-td { font-weight: bold; color: #0288d1; }
.detail-td { color: #546e7a; font-size: 0.9rem; }
.comment-td { font-size: 0.9rem; color: #78909c; font-style: italic; }
.comment-td.has-comment { color: #e65100; font-weight: 500; font-style: normal; }

/* ステータスバッジ */
.status-badge { display: inline-block; padding: 4px 8px; border-radius: 12px; font-size: 0.8rem; font-weight: bold; }
.status-pending { background-color: #fff3e0; color: #f57c00; }
.status-rejected { background-color: #ffebee; color: #d32f2f; }
.status-approved { background-color: #e8f5e9; color: #388e3c; }

.empty-table-alert { text-align: center; padding: 25px; color: #90a4ae; font-style: italic; background: #fafafa; border-radius: 4px; }

```

---

```css
/* フォームのデザイン */
.plan-form-card {
  background: white;
  padding: 25px;
  border-radius: 8px;
  border: 1px solid #b0bec5;
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.plan-form-card h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #263238;
  font-size: 1.1rem;
  border-left: 4px solid #007bff;
  padding-left: 10px;
}
.form-group { margin-bottom: 18px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: bold; color: #455a64; font-size: 0.9rem; }
.form-group input[type="date"], .form-group input[type="text"], .form-group textarea {
  width: 100%; padding: 10px; border: 1px solid #cfd8dc; border-radius: 4px; box-sizing: border-box; font-size: 0.95rem;
}
.required { color: #d32f2f; }
.btn-submit {
  width: 100%;
  background-color: #007bff;
  color: white;
  border: none;
  padding: 12px;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-submit:hover { background-color: #0056b3; }
</style>

```

---

### 双方向キャッチボールの最終確認手順

すべてのファイルを保存したら、フロントとバックを繋いだテストを行ってみましょう！

* **【一般ユーザー】申請を出す:**
一般ユーザー画面の「行動予定申請・管理」画面を開きます。

  * まだ申請が0件の場合でも、「履歴一覧」の枠組みが「データが見つかりませんでした」として綺麗に表示されます。
  * 未来の日付で「リモートワーク / 終日在宅」として申請を出します。
  * **提出した瞬間に、上の履歴一覧テーブルに「申請中」の行がリアルタイム追加**されます！

* **【管理者】コントロールパネルで裁定を下す:**
管理者アカウントでログインし、コントロールパネルの新しいタブ「予定申請確認」を開きます。

  * 先ほど一般ユーザーが出したカードが出現しています。
  * 「差戻す」を押し、コメント欄に「この日はオフィス出社日です」と入力して確定させます。

---

* **【一般ユーザー】結果を検索テーブルで確認する:**
再び一般ユーザーの申請履歴画面に戻ります。

  * 先ほどの行が **「差戻し」へと状態変化**し、伝言列に **「この日はオフィス出社日です」** と鮮やかに表示されます！
  * セレクトボックスを「承認済み」に変えるとテーブルがフッと空になり、「申請中」や「差戻し」に切り替えると対象データだけが吸い寄せられるように絞り込まれます。

### パート5：`accountId`を`userName`に差し替える

過日に「打刻内容編集申請」で行った**DTO（Data Transfer Object）を活用したガッチャンコ（データ結合）トラフィック**の設計思想を、この予定申請（`FuturePlan`）にも横展開しましょう！

Repositoryの構造を壊すことなく、ServiceとControllerをアップデートして**ユーザー名付きの予定リスト**を返却するバックエンド側の修正コードを提示します。

---

#### DTOを活用したマージ（結合）トラフィックのイメージ

```text
1. Repository から「planStatus = 0（申請中）」の生データを掘り起こす（この時点ではUUIDのみ）
2. 各予定の accountId を手掛かりに、UserAccountRepository等から「ユーザー名」を引き抜く
3. 新設する `FuturePlanDTO` にすべての予定データとユーザー名を詰め込んでフロントへ返却！
```

#### 1. DTOクラスの新設：`FuturePlanDTO.java`

`com.appspace.backend.dto` パッケージ（または以前 `TimeChangeRequest` などを配置したパッケージ）に新規作成します。

```java
package com.appspace.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FuturePlanDTO {
```

---

```java
    private String planId;
    private String accountId;
    private String userName; // これが画面に表示される待望のユーザー名枠です！
    private LocalDate planDate;
    private String planTitle;
    private String planDetail;
    private Integer planStatus;
    private String adminComment;
    private LocalDateTime createdAt;
}
```

#### 2. Service層の修正：`FuturePlanService.java`

管理者用に「未承認一覧を取得するメソッド」を、生エンティティのリストから、名前をマージした **`List<FuturePlanDTO>`** を返却するロジックへアップデートします。

※以前の打刻申請の際と同様、アカウントの情報を保持しているリポジトリ（例: `UserAccountRepository` や `UserRepository` など、お使いの環境のクラス名）を `@Autowired` して連携させてください。

---

```java
// --- FuturePlanService.java の上部にアカウント用リポジトリをインポート・接続します ---
// import com.appspace.backend.repository.UserAccountRepository; // お使いのアカウントリポジトリ名

import com.appspace.backend.dto.FuturePlanDTO; // ★追加

// 〜 クラス内部 〜

    @Autowired
    private FuturePlanRepository futurePlanRepository;

    // @Autowired
    // private UserAccountRepository userAccountRepository; // ★お使いのアカウントリポジトリを接続

    /**
     * 管理者ロジック: すべての「申請中 (0)」の予定に、ユーザー名を結合したDTOリストとして取得する
     */
    public List<FuturePlanDTO> getPendingPlansForAdmin() {
        // 1. まずは通常通りステータス0の生の予定リストを取得
        List<FuturePlan> rawPlans = futurePlanRepository.findByPlanStatusOrderByPlanDateAsc(0);

        // 2. Stream API を使って、1件ずつ名前をドッキングしてDTOへ詰め替える
        return rawPlans.stream().map(plan -> {
            FuturePlanDTO dto = new FuturePlanDTO();
            dto.setPlanId(plan.getPlanId());
            dto.setAccountId(plan.getAccountId());
            dto.setPlanDate(plan.getPlanDate());
            dto.setPlanTitle(plan.getPlanTitle());
            dto.setPlanDetail(plan.getPlanDetail());
            dto.setPlanStatus(plan.getPlanStatus());
            dto.setAdminComment(plan.getAdminComment());
            dto.setCreatedAt(plan.getCreatedAt());

```

---

```java
            // ユーザーアカウントテーブルから名前を引き去り、ドッキング！
            dto.setUserName("不明な社員");
            try {
              String targetId = plan.getAccountId();

              userAccountRepository.findByAccountId(targetId).ifPresent(acc -> {
                dto.setUserName(acc.getUserName());
              });
            } catch (Exception e) {
              // セーフティ
            }

            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

```

#### 3. Controller層の修正：`FuturePlanController.java`

管理者用の未承認リスト取得窓口（`窓口4`）の戻り値の型を `List<FuturePlanDTO>` に書き換えます。

---

```java
// --- FuturePlanController.java の対象メソッドを書き換え ---

    /**
     * 窓口4: 管理者用：現在届いている「未承認の予定申請」を名前結合版DTOで一覧取得する
     * GET http://localhost:8080/api/admin/plans/pending-list
     */
    @GetMapping("/admin/plans/pending-list")
    public ResponseEntity<List<com.appspace.backend.dto.FuturePlanDTO>> getAdminPendingPlans() {
        try {
            // Service側でDTOに変換された綺麗なリストがそのまま返却されます
            List<com.appspace.backend.dto.FuturePlanDTO> dtoList = futurePlanService.getPendingPlansForAdmin();
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

```

#### 4. フロントエンド（Vue）側の仕上げ：`AdminFuturePlanPanel.vue`

バックエンドから `userName` が届くようになったので、フロントの表示箇所を書き換えるだけです。

`AdminFuturePlanPanel.vue` の `<template>` のヘッダー部分を以下のように修正してください。

---

```html
<div class="card-header">
  <span class="user-id">申請者: {{ plan.userName }}</span>
  <span class="target-date">対象日: {{ formatPlanDate(plan.planDate) }}</span>
</div>
```

### 連動の確認

1. バックエンドを上書き保存し、再起動（`mvnw clean spring-boot:run`）します。
2. 管理者画面にログインし、「予定申請確認」タブを開きます。

これで、`accountId` のUUID表記から、JPAの紐付けによって引き出された **`申請者: 社員名（または設定したダミー名）`** に置き換われば、データ結合リファクタリングは成功です！
