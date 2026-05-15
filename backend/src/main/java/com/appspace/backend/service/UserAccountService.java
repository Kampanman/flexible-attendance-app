package com.appspace.backend.service;

import com.appspace.backend.entity.UserAccount;
import com.appspace.backend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.List;  // ←インポートをお忘れなく

@Service
@RequiredArgsConstructor  // これをつけることで、repository を自動的に繋いでくれる（依存性の注入）コードが生成される
@Transactional  // データベースの処理中にエラーが起きたら、処理を自動でキャンセル（ロールバック）してデータが壊れるのを防ぐ
public class UserAccountService {

    private final UserAccountRepository repository;
    
    @Autowired
    private PasswordEncoder passwordEncoder; // 直接 new せず、Springに管理されている部品をもらう

    /**
     * 新規ユーザー登録（パスワードをハッシュ化して保存）
     */
    public UserAccount registerUser(UserAccount account) {
        // 1. メールアドレス（userId）の重複チェック
        if (repository.findByUserId(account.getUserId()).isPresent()) {
            throw new RuntimeException("このメールアドレスは既に登録されています。");
        }

        // 2. パスワードをBCryptでハッシュ化してセット
        String encodedPassword = passwordEncoder.encode(account.getPassword());
        account.setPassword(encodedPassword);

        // 3. データベースへ保存
        return repository.save(account);
    }

    /**
     * ログインID（メールアドレス）でユーザーを探す
     */
    public Optional<UserAccount> findByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    /**
     * ユーザー認証（ログインチェック）
     * ハッシュ化されたパスワード（$2a$10$...）と、ユーザーが入力した生のパスワード（mysecretpassword）を安全に照合するために設けている
     * このサービスを呼び出すための「ログイン窓口（エンドポイント）」は、UserAccountController.javaに設けられている
     * @param userId ログインID（メールアドレス）
     * @param rawPassword 入力された生のパスワード
     * @return 認証成功時はユーザー情報、失敗時は空
     */
    public Optional<UserAccount> authenticate(String userId, String rawPassword) {
        return repository.findByUserId(userId)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    /**
     * 全ユーザーの一覧を取得する（デバッグ用）
     */
    public List<UserAccount> findAll() {
        return repository.findAll();
    }

    /**
     * ログイン画面を経由してのログイン
     * @param userId
     * @param rawPassword
     * @return
     */
    public UserAccount login(String userId, String rawPassword) {
        UserAccount user = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // rawPassword（入力された平文）と user.getPassword()（DBのハッシュ化済み）を比較
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return user;
        } else {
            throw new RuntimeException("Invalid password");
        }
    }

    // ユーザーの新規登録
    public void registerNewUser(UserAccount user) {
        // 1. 重複チェック
        if (repository.existsByUserId(user.getUserId())) {
            throw new RuntimeException("このメールアドレスは既に登録されています。");
        }

        // 2. パスワードのハッシュ化
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 3. デフォルト値のセット（必要に応じて）
        user.setIsAuth(0); // 一般ユーザーとして登録
        user.setQuitDemand(0); // 退会フラグはオフ
        
        // 4. 保存 (accountIdとcreatedAtは@PrePersistで自動生成される)
        repository.save(user);
    }
}