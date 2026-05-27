package com.appspace.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.entity.AttendanceRecord;
import com.appspace.backend.service.AttendanceRecordService;

import lombok.RequiredArgsConstructor;

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
     * ステータス確認API の実装のため追加
     * 現在のステータスを確認する
     * GET http://localhost:8080/api/attendance/status?accountId=ユーザーID
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus(@RequestParam String accountId) {
        String change_status = attendanceService.getAttendanceStatus(accountId);

        // 文字列をそのまま返すとJSONとして扱いにくいため、シンプルなテキストで返します
        return ResponseEntity.ok(change_status);
    }

    /**
     * 打刻履歴の取得API の実装のため追加
     * 打刻履歴一覧を取得する
     * GET http://localhost:8080/api/attendance/history/{accountId=ユーザーID}
     */
    @GetMapping("/history/{accountId}")
    public ResponseEntity<List<AttendanceRecord>> getHistory(@PathVariable String accountId) {
        List<AttendanceRecord> history = attendanceService.getHistoryByAccountId(accountId);
        return ResponseEntity.ok(history);
    }

    /**
     * 打刻リクエストを受け取ってDBに保存するメソッド
     * 
     * @param request
     * @return
     */
    @PostMapping("/punch")
    public ResponseEntity<String> punch(@RequestBody Map<String, String> request) {
        String accountId = request.get("accountId");
        String type = request.get("type"); // CLOCK_IN または CLOCK_OUT

        // サービスを呼び出して保存処理を実行
        try {
            attendanceService.savePunch(accountId, type);
            return ResponseEntity.ok("打刻に成功しました");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("打刻失敗: " + e.getMessage());
        }
    }

}