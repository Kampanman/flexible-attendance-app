package com.appspace.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.entity.UserAccount;
import com.appspace.backend.service.UserAccountService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController // 「画面（HTML）」ではなく「データ（JSON）」を返す窓口であることを示す（htmlの場合は@Controller）
@RequestMapping("/api/users") // コントローラーが扱うURLの共通ルート
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
/**
 * GitHub Codespacesでは、フロントエンド（Vue.js）とバックエンド（Spring Boot）でURLが異なる
 * 
 * @CrossOriginの設定がないとセキュリティ制限で通信がブロックされることになる
 */
public class UserAccountController {

    private final UserAccountService userService;

    @Autowired
    private Logger logger;

    /**
     * ユーザー新規登録を受け付けるエンドポイント
     * POST http://localhost:8080/api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserAccount account) {
        // @RequestBodyで、届いたJSONデータ（ユーザー名やパスワードなど）を、自動的に UserAccount オブジェクトに変換して取り込む
        try {
            userService.registerUser(account);
            String completeMessage = "ユーザー登録が完了しました";
            return ResponseEntity.ok(completeMessage);
        } catch (RuntimeException e) {
            // 重複エラーなどが起きた場合は、400 Bad Request を返す
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ログイン処理を受け付けるエンドポイント
     * POST http://localhost:8080/api/users/login
     * SecurityConfig.javaに、このエンドポイントを許可する設定を追加している
     */
    @PostMapping("/login")
    public ResponseEntity<UserAccount> login(@RequestBody UserAccount loginRequest) {
        return userService.authenticate(loginRequest.getUserId(), loginRequest.getPassword())
                .map(user -> {
                    // 最終ログイン日時を更新するなどの処理をここに書くことも可能
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.status(401).build()); // 認証失敗時は 401 Unauthorized
    }

    /**
     * データベースの中身を確認するために 「一時的な確認用API」
     * POST http://localhost:8080/api/users/debug-list
     */
    @GetMapping("/debug-list")
    public List<UserAccount> debugList() {
        return userService.findAll(); // findAll()メソッドをServiceに作る必要があります
    }

    /**
     * アカウント情報の更新を受け付けるエンドポイント
     * PUT http://localhost:8080/api/users/update
     */
    @PutMapping("/update")
    public ResponseEntity<String> updateAccount(@RequestBody UserAccount accountRequest) {
        try {
            // サービス層の更新処理を呼び出す
            userService.updateUser(accountRequest);

            // Vue側で「更新成功」をトリガーにするためのメッセージを返す
            return ResponseEntity.ok("アカウント情報を更新しました。");
        } catch (RuntimeException e) {
            // ユーザーが見つからないなどのエラー時は 400 Bad Request
            return ResponseEntity.badRequest().body("更新に失敗しました: " + e.getMessage());
        }
    }

    /**
     * アカウント削除申請（退会申請）を受け付けるエンドポイント
     * PUT http://localhost:8080/api/users/quit
     */
    @PutMapping("/quit")
    public ResponseEntity<String> quitAccount(@RequestBody AccountUpdateRequest quitRequest) {
        try {
            logger.info("=== 退会申請リクエストを受信 ===");
            logger.info("    accountId: {}", quitRequest.getAccountId());

            // 1. セーフティガード：対象のアカウント情報を取得
            UserAccount targetUser = userService.findByAccountId(quitRequest.getAccountId());

            // 2. もし対象が初期統括管理者のメールアドレスだったら申請を強制ブロックする
            if ("admin@example.com".equals(targetUser.getUserId())) {
                logger.warn("[警告] 統括管理者に対する退会申請が拒絶されました。");
                return ResponseEntity.badRequest().body("エラー：統括管理者アカウントは削除できません。");
            }

            // 3. 安全が確認されたらサービス層の退会申請処理を呼び出す
            userService.applyQuitDemand(quitRequest.getAccountId());

            return ResponseEntity.ok("アカウント削除申請を受け付けました。");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("申請に失敗しました: " + e.getMessage());
        }
    }

    /**
     * 管理者用：すべてのアカウント一覧を取得するエンドポイント
     * GET http://localhost:8080/api/users/admin/list
     */
    @GetMapping("/admin/list")
    public ResponseEntity<List<UserAccount>> getAdminUserList() {
        // サービス層から全ユーザーを取得して返却
        List<UserAccount> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * 管理者用：退会申請を承認（アカウント削除）するエンドポイント
     * DELETE http://localhost:8080/api/users/approve-quit/{accountId}
     */
    @DeleteMapping("/approve-quit/{accountId}")
    public ResponseEntity<String> approveQuit(@PathVariable String accountId) {
        try {
            logger.info("=== 退会承認リクエストを受信 ===");
            logger.info("   対象accountId: {}", accountId);

            // サービス層の削除処理を呼び出し
            userService.approveQuitDemand(accountId);

            return ResponseEntity.ok("退会申請を承認し、アカウントを削除しました。");
        } catch (RuntimeException e) {
            // エラー時は 400 Bad Request を返却
            return ResponseEntity.badRequest().body("承認処理に失敗しました: " + e.getMessage());
        }
    }

    /**
     * アカウント更新リクエスト専用のデータ構造（DTO）
     */
    @Getter
    @Setter
    public static class AccountUpdateRequest {
        private String accountId;
        private String userName;
        private String password;
    }
}