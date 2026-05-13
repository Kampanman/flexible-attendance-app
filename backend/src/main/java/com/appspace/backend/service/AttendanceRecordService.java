package com.appspace.backend.service;

import com.appspace.backend.entity.AttendanceRecord;
import com.appspace.backend.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceRecordService {

    private final AttendanceRecordRepository repository;

    /**
     * 出勤打刻
     */
    public AttendanceRecord clockIn(String accountId) {
        // 最新のレコードを確認し、既に出勤中（退勤していない）ならエラーにする
        repository.findFirstByAccountIdOrderByIdDesc(accountId)
                .ifPresent(lastRecord -> {
                    if (lastRecord.getClockOut() == null) {
                        throw new RuntimeException("既に提出済みの出勤記録があります。退勤を先に完了してください。");
                    }
                });

        AttendanceRecord record = new AttendanceRecord();
        record.setAccountId(accountId);
        record.setClockIn(LocalDateTime.now());
        record.setStatus("出勤中");
        return repository.save(record);
    }

    /**
     * 退勤打刻
     */
    public AttendanceRecord clockOut(String accountId) {
        // 最新のレコードを探し、出勤データがない、または既に退勤済みならエラーにする
        AttendanceRecord lastRecord = repository.findFirstByAccountIdOrderByIdDesc(accountId)
                .orElseThrow(() -> new RuntimeException("出勤記録が見つかりません。"));

        if (lastRecord.getClockOut() != null) {
            throw new RuntimeException("既に退勤済みです。");
        }

        // 既存のレコードに退勤時刻を書き込む
        lastRecord.setClockOut(LocalDateTime.now());
        lastRecord.setStatus("退勤済み");
        return repository.save(lastRecord);
    }

    /**
     * 打刻履歴の取得API の実装のため追加
     * 特定のユーザーの全打刻履歴を取得する
     */
    public java.util.List<AttendanceRecord> getAllRecords(String accountId) {
        return repository.findByAccountIdOrderByClockInDesc(accountId);
    }

    /**
     * ステータス確認API の実装のため追加
     * 現在の打刻ステータスを取得する
     * @return "CLOCKED_IN" (出勤中), "CLOCKED_OUT" (退勤済み/未出勤)
     */
    public String getCurrentStatus(String accountId) {
        return repository.findFirstByAccountIdOrderByIdDesc(accountId)
                .map(record -> {
                    if (record.getClockOut() == null) {
                        return "CLOCKED_IN";
                    } else {
                        return "CLOCKED_OUT";
                    }
                })
                .orElse("CLOCKED_OUT"); // 記録が一つもない場合
    }

    public List<AttendanceRecord> getHistory(String accountId) {
        return repository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}