package com.appspace.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;

@Entity
@Table(name = "apply_attendance_records")
@Data
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId; // UserAccountのaccountIdと紐付け

    private LocalDateTime clockIn;  // 出勤時刻
    private LocalDateTime clockOut; // 退勤時刻

    @Column(length = 20)
    private String status; // 例: "出勤中", "退勤済み"

    private String memo;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}