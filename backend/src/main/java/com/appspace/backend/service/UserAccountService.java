package com.appspace.backend.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appspace.backend.entity.UserAccount;
import com.appspace.backend.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // これをつけることで、repository を自動的に繋いでくれる（依存性の注入）コードが生成される
@Transactional // データベースの処理中にエラーが起きたら、処理を自動でキャンセル（ロールバック）してデータが壊れるのを防ぐ
public class UserAccountService {

    private final UserAccountRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 直接 new せず、Springに管理されている部品をもらう

    @Autowired
    private Logger logger;

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
     * 
     * @param userId      ログインID（メールアドレス）
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
     * 
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

    /**
     * アカウント情報の更新処理
     * 
     * @param requestData フロントから届いた更新用データ（accountId, userName, passwordを含む）
     * @return 更新後のユーザー情報
     */
    public UserAccount updateUser(UserAccount requestData) {
        // 1. accountId を基に、既存のユーザーをデータベースから検索
        UserAccount existingUser = repository.findById(requestData.getAccountId())
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません。"));

        // 2. ユーザー名の更新
        if (requestData.getUserName() != null && !requestData.getUserName().trim().isEmpty()) {
            existingUser.setUserName(requestData.getUserName().trim());
        }

        // 3. パスワードの更新チェック
        if (requestData.getPassword() != null && !requestData.getPassword().trim().isEmpty()) {
            // パスワードが入力されている（nullでも空文字でもない）場合のみ、ハッシュ化して上書きする
            String encodedPassword = passwordEncoder.encode(requestData.getPassword());
            existingUser.setPassword(encodedPassword);
        }

        // 4. 自己紹介（about）など、もし他の編集可能フィールドがあれば同様にマッピング
        if (requestData.getAbout() != null) {
            existingUser.setAbout(requestData.getAbout());
        }

        // 5. データベースへ上書き保存（JPAの仕様により、既存レコードへのsaveはUPDATE文になります）
        return repository.save(existingUser);
    }

    /**
     * アプリ起動時に初期管理者アカウントが存在しない場合、自動生成する処理
     */
    public void initializeAdminAccount() {
        String adminEmail = "admin@example.com"; // 任意の管理用メールアドレス

        // 既に管理者が存在するかチェック
        if (!repository.findByUserId(adminEmail).isPresent()) {
            UserAccount admin = new UserAccount();
            admin.setUserName("統括管理者");
            admin.setUserId(adminEmail);
            // 初期パスワード（安全にハッシュ化します）
            admin.setPassword(passwordEncoder.encode("admin1234"));
            admin.setIsAuth(1); // 1: 管理者権限を付与
            admin.setQuitDemand(0); // 退会申請はオフ
            admin.setAbout("システム初期設定によって作成された最高管理者アカウントです。");

            repository.save(admin);

            logger.info("=== [System] 初期管理者アカウント(admin@example.com)を作成しました ===");
        }
    }

    /**
     * 退会申請（アカウント削除申請）の処理
     * 
     * @param accountId 対象のアカウントID
     */
    public void applyQuitDemand(String accountId) {
        UserAccount user = repository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません。"));

        // 退会申請フラグを 1 (オン) に更新
        user.setQuitDemand(1);
        repository.save(user);
    }
}