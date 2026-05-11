package com.appspace.backend.controller;

import com.appspace.backend.entity.UserAccount;
import com.appspace.backend.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController  // 「画面（HTML）」ではなく「データ（JSON）」を返す窓口であることを示す（htmlの場合は@Controller）
@RequestMapping("/api/users")  // コントローラーが扱うURLの共通ルート
@RequiredArgsConstructor
@CrossOrigin(origins = "https://*.app.github.dev") // Codespacesのフロントエンドからのアクセスを許可
/**
 * GitHub Codespacesでは、フロントエンド（Vue.js）とバックエンド（Spring Boot）でURLが異なる
 * @CrossOriginの設定がないとセキュリティ制限で通信がブロックされることになる
 */ 
public class UserAccountController {

    private final UserAccountService userService;

    /**
     * ユーザー新規登録を受け付けるエンドポイント
     * POST http://localhost:8080/api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<UserAccount> register(@RequestBody UserAccount account) {
        // @RequestBodyで、届いたJSONデータ（ユーザー名やパスワードなど）を、自動的に UserAccount オブジェクトに変換して取り込む
        try {
            UserAccount savedAccount = userService.registerUser(account);
            return ResponseEntity.ok(savedAccount);
        } catch (RuntimeException e) {
            // 重複エラーなどが起きた場合は、400 Bad Request を返す
            return ResponseEntity.badRequest().body(null);
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
}