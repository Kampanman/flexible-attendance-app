package com.appspace.backend.controller;

import java.util.List; // ←インポートをお忘れなく

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.entity.UserAccount;
import com.appspace.backend.service.UserAccountService;

import lombok.RequiredArgsConstructor;

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

    /**
     * ユーザー新規登録を受け付けるエンドポイント
     * POST http://localhost:8080/api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserAccount account) {
        // @RequestBodyで、届いたJSONデータ（ユーザー名やパスワードなど）を、自動的に UserAccount オブジェクトに変換して取り込む
        try {
            UserAccount savedAccount = userService.registerUser(account);
            // return ResponseEntity.ok(savedAccount);
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
}