package com.appspace.backend.repository;

import com.appspace.backend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    
    // 特定のユーザーの、最新の打刻記録を1件だけ取得する
    // これにより「前回出勤したのか、退勤したのか」を判定できます
    Optional<AttendanceRecord> findFirstByAccountIdOrderByIdDesc(String accountId);

    // 打刻履歴の取得API の実装のため追加
    java.util.List<AttendanceRecord> findByAccountIdOrderByClockInDesc(String accountId);
}