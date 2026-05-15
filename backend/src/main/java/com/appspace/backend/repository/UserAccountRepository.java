package com.appspace.backend.repository;

import com.appspace.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    /**
     * JpaRepository<UserAccount, String>: これを継承するだけで、save()（保存）、findAll()（全件取得）、deleteById()（削除）といった標準機能が自動的に使えるようになる
     */
    
    // メールアドレス（userId）での重複チェック
    boolean existsByUserId(String userId); 

    // 仕様書に基づき、ログインID（メールアドレス）でユーザーを検索するメソッドを追加[cite: 1]
    Optional<UserAccount> findByUserId(String userId);  // Optionalなのは、ユーザーが見つからなかった場合に「NULL」ではなく「空の状態」として安全に扱えるようするため

    // ユーザー名が含まれているものを検索する（管理者用のアカウント検索などで利用）[cite: 1]
    java.util.List<UserAccount> findByUserNameContaining(String userName);
}