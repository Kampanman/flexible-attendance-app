package com.appspace.backend.controller;

import com.appspace.backend.entity.AttendanceRecord;
import com.appspace.backend.service.AttendanceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://*.app.github.dev")
public class AttendanceController {

    private final AttendanceRecordService attendanceService;

    /**
     * 出勤打刻を受け付ける
     * POST http://localhost:8080/api/attendance/clock-in?accountId=ユーザーID
     */
    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceRecord> clockIn(@RequestParam String accountId) {
        try {
            AttendanceRecord record = attendanceService.clockIn(accountId);
            return ResponseEntity.ok(record);
        } catch (RuntimeException e) {
            // 既に二重出勤などのエラーが発生した場合
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 退勤打刻を受け付ける
     * POST http://localhost:8080/api/attendance/clock-out?accountId=ユーザーID
     */
    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceRecord> clockOut(@RequestParam String accountId) {
        try {
            AttendanceRecord record = attendanceService.clockOut(accountId);
            return ResponseEntity.ok(record);
        } catch (RuntimeException e) {
            // 出勤データがない、または既に退勤済みの場合
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 打刻履歴の取得API の実装のため追加
     * 打刻履歴一覧を取得する
     * GET http://localhost:8080/api/attendance/history?accountId=ユーザーID
     */
    @GetMapping("/history")
    public ResponseEntity<java.util.List<AttendanceRecord>> getHistory(@RequestParam String accountId) {
        java.util.List<AttendanceRecord> history = attendanceService.getAllRecords(accountId);
        return ResponseEntity.ok(history);
    }

    /**
     * ステータス確認API の実装のため追加
     * 現在のステータスを確認する
     * GET http://localhost:8080/api/attendance/status?accountId=ユーザーID
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus(@RequestParam String accountId) {
        String status = attendanceService.getCurrentStatus(accountId);
        // 文字列をそのまま返すとJSONとして扱いにくいため、シンプルなテキストで返します
        return ResponseEntity.ok(status);
    }
}