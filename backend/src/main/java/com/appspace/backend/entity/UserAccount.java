package com.appspace.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "apply_user_accounts")

@Data  // GetterやSetterを自分で書く必要がなくなる
public class UserAccount {

    @Id
    @Column(length = 36)
    private String accountId; // UUIDを利用した16桁〜36桁の値[cite: 1]

    @Column(nullable = false, length = 30)
    private String userName;

    @Column(nullable = false, unique = true)
    private String userId; // メールアドレス[cite: 1]

    @Column(nullable = false)
    private String password; // BCryptハッシュ化済みパスワード[cite: 1]

    @Column(nullable = false)
    private int isAuth = 0; // 0:一般, 1:管理者[cite: 1]

    @Column(nullable = false)
    private int quitDemand = 0; // 退会申請フラグ[cite: 1]

    @Column(columnDefinition = "TEXT")
    private String about;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // レコード作成前に自動でUUIDを生成・日時をセットする
    @PrePersist  // データベースに保存される直前に、Java側で自動的にUUIDと作成日時を生成する
    protected void onCreate() {
        if (this.accountId == null) {
            this.accountId = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
    }
}