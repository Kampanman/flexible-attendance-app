package com.appspace.backend.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.entity.EntryExitCalendar;
import com.appspace.backend.service.EntryExitCalendarService;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "https://*.app.github.dev") // Codespaces環境のCORS対策
public class AttendanceEditController {

  @Autowired
  private Logger logger;

  @Autowired
  private EntryExitCalendarService calendarService; // ビジネスロジックの職人を呼び出せるように接続

  /**
   * 窓口1: ユーザー自身の指定年月の1か月分のカレンダーリストを返却する
   * GET
   * http://localhost:8080/api/attendance/monthly-list?accountId=xxx&yearMonth=2026-05
   */
  @GetMapping("/monthly-list")
  public ResponseEntity<?> getMonthlyList(
      @RequestParam String accountId,
      @RequestParam String yearMonth) {
    try {
      // 1か月分リストの生成
      logger.info("[getMonthlyList] accountId: {}, yearMonth: {}", accountId, yearMonth);
      List<EntryExitCalendar> list = calendarService.getMonthlyCalendar(accountId, yearMonth);
      return ResponseEntity.ok(list);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("カレンダーデータの取得に失敗しました: " + e.getMessage());
    }
  }

  /**
   * 窓口2: ユーザーからの打刻修正（編集）申請リクエストを受け付ける
   * POST http://localhost:8080/api/attendance/request-edit
   */
  @PostMapping("/request-edit")
  public ResponseEntity<String> requestEdit(@RequestBody com.appspace.backend.dto.TimeChangeRequest request) {

    // 1. 備考（修正理由）の空っぽチェック
    if (request.getReason() == null || request.getReason().trim().isEmpty()) {
      return ResponseEntity.badRequest().body("【却下】修正理由（備考）の入力は必須です。");
    }

    // 2. 日付判定の準備
    java.time.LocalDate today = java.time.LocalDate.now();
    java.time.LocalDate targetDate = request.getRecordDate();
    boolean isToday = targetDate.equals(today);

    // =================================================================
    // 【新規追加：期間統制の二重防壁】
    // =================================================================
    if (!isToday) {
      // 制限1: 操作日当日以外の場合、申請ができるのは「当日の1ヶ月前」の同日まで
      java.time.LocalDate oneMonthAgo = today.minusMonths(1);
      if (targetDate.isBefore(oneMonthAgo)) {
        return ResponseEntity.badRequest().body("【却下】過去の打刻内容編集申請は、1ヶ月前のシステム許容期間内の日付に限られます。");
      }

      // 制限2: 安全策として、表示上限である「3ヶ月後」を超える未来の申請もブロック
      java.time.LocalDate threeMonthsLater = today.plusMonths(3);
      if (targetDate.isAfter(threeMonthsLater)) {
        return ResponseEntity.badRequest().body("【却下】未来すぎる日付への申請は受け付けられません。");
      }
    }
    // =================================================================

    // 3. 時刻形式チェック用の正規表現 (hh:mm:ss)
    String timeRegex = "^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$";

    // --- Entry（出勤）時刻のチェック ---
    if (request.getTargetEntryTime() == null ||
        request.getTargetEntryTime().equals("-") ||
        request.getTargetEntryTime().trim().isEmpty()) {
      return ResponseEntity.badRequest().body("【却下】出勤時刻(Entry)を入力してください。");
    }
    if (!request.getTargetEntryTime().matches(timeRegex)) {
      return ResponseEntity.badRequest().body("【却下】Entry時刻の形式が不正です。(hh:mm:ssで入力してください)");
    }

    // --- Exit（退勤）時刻のチェック ---
    String exitTime = request.getTargetExitTime();
    if (exitTime == null || exitTime.trim().isEmpty()) {
      exitTime = "-";
    }

    if (isToday) {
      // 当日の場合: 退勤時刻が「-」であっても許容
      if (!exitTime.equals("-") && !exitTime.matches(timeRegex)) {
        return ResponseEntity.badRequest().body("【却下】当日の退勤時刻(Exit)の形式が不正です。");
      }
    } else {
      // 前日以前（過去）の場合: 退勤時刻「-」は不可
      if (exitTime.equals("-")) {
        return ResponseEntity.badRequest().body("【却下】過去の日付の申請には、退勤時刻(Exit)の入力も必須です。");
      }
      if (!exitTime.matches(timeRegex)) {
        return ResponseEntity.badRequest().body("【却下】Exit時刻の形式が不正です。(hh:mm:ssで入力してください)");
      }
    }

    try {
      calendarService.requestTimeChange(
          request.getAccountId(),
          request.getRecordDate(),
          request.getTargetEntryTime(),
          exitTime,
          request.getReason());

      logger.info("=== [Controller] 期間統制チェックを通過し、修正申請を受付完了 ===");
      return ResponseEntity.ok("打刻内容の修正申請を提出しました。管理者の承認をお待ちください。");

    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("申請処理中に重大なエラーが発生しました: " + e.getMessage());
    }
  }

  /**
   * 窓口3: ユーザーからの打刻修正申請の「取り消し」リクエストを受け付ける
   * POST http://localhost:8080/api/attendance/cancel-edit
   */
  @PostMapping("/cancel-edit")
  public ResponseEntity<String> cancelEdit(@RequestBody Map<String, Object> payload) {
    String accountId = (String) payload.get("accountId");
    String dateStr = (String) payload.get("recordDate");

    if (accountId == null || dateStr == null) {
      return ResponseEntity.badRequest().body("必要なパラメータが不足しています。");
    }

    try {
      java.time.LocalDate recordDate = java.time.LocalDate.parse(dateStr);
      logger.info("[cancelEdit] accountId:{}, recordDate:{}", accountId, recordDate);
      calendarService.cancelTimeChangeRequest(accountId, recordDate);
      return ResponseEntity.ok("申請を取り消しました。");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("取り消し処理中にエラーが発生しました: " + e.getMessage());
    }
  }
}