package com.appspace.backend.service;

import com.appspace.backend.entity.AttendanceRecord;
import com.appspace.backend.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

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
     * 特定のユーザーの全打刻履歴を、新しい順に取得する
     */
    public List<AttendanceRecord> getHistoryByAccountId(String accountId) {
        // Repository のメソッドを呼び出します
        return repository.findByAccountIdOrderByCreatedAtDesc(accountId);
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

    public void savePunch(String accountId, String type) {
        // 1. 手元の記述通り「AttendanceRecord」のインスタンスを生成
        AttendanceRecord record = new AttendanceRecord();
        record.setAccountId(accountId);
        
        // null安全のための対策（前後の余白削除）
        String punchType = (type != null) ? type.trim() : "";

        // 2. 大文字・小文字、ハイフンの有無をすべて吸収して条件分岐
        if ("CLOCK_IN".equalsIgnoreCase(punchType) || "clock-in".equalsIgnoreCase(punchType)) {
            record.setType("clock-in");             // DB内での文字列を小文字に統一
            record.setClockIn(LocalDateTime.now());  // ★入室時刻（clockIn）に現在日時をセット
            record.setStatus("CLOCKED_IN");
        } else if ("CLOCK_OUT".equalsIgnoreCase(punchType) || "clock-out".equalsIgnoreCase(punchType)) {
            record.setType("clock-out");
            record.setClockOut(LocalDateTime.now()); // ★退室時刻（clockOut）に現在日時をセット
            record.setStatus("CLOCK_OUT");
        } else {
            // 想定外の文字列が届いた場合は例外を投げてController側のcatchに落とす
            throw new IllegalArgumentException("不正な打刻タイプです: " + type);
        }

        // 3. 手元の記述通り「repository」を利用してデータベースへ保存
        // ※ @PrePersist による createdAt の自動セット機能もそのまま活きて働きます
        repository.save(record);
    }

    public String getAttendanceStatus(String accountId) {
        // 最新の1件を取得するリポジトリメソッドを呼び出す
        Optional<AttendanceRecord> lastRecord = repository.findFirstByAccountIdOrderByCreatedAtDesc(accountId);

        if (lastRecord.isEmpty()) {
            return "CLOCKED_OUT"; // 履歴がなければ未出勤
        }

        // 最新のレコードのタイプによってステータスを返す
        if ("clock-in".equals(lastRecord.get().getType())) {
            return "CLOCKED_IN";
        } else {
            return "CLOCKED_OUT";
        }
    }
}