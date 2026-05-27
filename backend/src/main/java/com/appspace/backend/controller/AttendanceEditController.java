package com.appspace.backend.controller;

import java.util.List;

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

import com.appspace.backend.dto.TimeChangeRequest;
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
      logger.info("accountId: {}, yearMonth: {}", accountId, yearMonth);
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
  public ResponseEntity<String> requestEdit(@RequestBody TimeChangeRequest request) {

    // 【二重バリデーション】仕様書要件に基づく厳格なバックエンドチェック

    // 1. 備考（修正理由）の空っぽチェック
    if (request.getReason() == null || request.getReason().trim().isEmpty()) {
      return ResponseEntity.badRequest().body("【却下】修正理由（備考）の入力は必須です。");
    }

    // 2. 時刻が正しい形式（hh:mm:ss）になっているか正規表現を使ってチェック
    // 「数字2桁 : 数字2桁 : 数字2桁」の形、または未打刻を埋めるハイフン「-」のみ許容
    String timeRegex = "^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$";

    if (!request.getTargetEntryTime().equals("-") && !request.getTargetEntryTime().matches(timeRegex)) {
      return ResponseEntity.badRequest().body("【却下】Entry時刻の形式が不正です。(hh:mm:ssで入力してください)");
    }
    if (!request.getTargetExitTime().equals("-") && !request.getTargetExitTime().matches(timeRegex)) {
      return ResponseEntity.badRequest().body("【却下】Exit時刻の形式が不正です。(hh:mm:ssで入力してください)");
    }

    try {
      // すべてのチェックをクリアしたら、サービス層にDBへの書き込みを命じます
      calendarService.requestTimeChange(
          request.getAccountId(),
          request.getRecordDate(),
          request.getTargetEntryTime(),
          request.getTargetExitTime(),
          request.getReason());

      logger.info("=== [Controller] 修正申請の受付に成功しました ===");
      return ResponseEntity.ok("打刻内容の修正申請を提出しました。管理者の承認をお待ちください。");

    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("申請処理中に重大なエラーが発生しました: " + e.getMessage());
    }
  }
}