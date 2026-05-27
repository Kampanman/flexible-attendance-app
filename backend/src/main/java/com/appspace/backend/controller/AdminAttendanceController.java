package com.appspace.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.entity.EntryExitCalendar;
import com.appspace.backend.service.AdminAttendanceService;

@RestController
@RequestMapping("/api/admin/attendance")
@CrossOrigin(origins = "https://*.app.github.dev")
public class AdminAttendanceController {

  @Autowired
  private AdminAttendanceService adminService;

  /**
   * 管理者窓口1: 未承認の編集申請一覧を一括取得する
   * GET http://localhost:8080/api/admin/attendance/requests
   */
  @GetMapping("/requests")
  public ResponseEntity<List<EntryExitCalendar>> getPendingRequests() {
    return ResponseEntity.ok(adminService.getPendingTimechangeRequests());
  }

  /**
   * 管理者窓口2: 申請を承認する
   * POST http://localhost:8080/api/admin/attendance/approve
   */
  @PostMapping("/approve")
  public ResponseEntity<String> approveRequest(@RequestBody Map<String, String> payload) {
    String recordId = payload.get("recordId");
    try {
      adminService.approveTimechange(recordId);
      return ResponseEntity.ok("申請を承認しました。打刻時間が更新されました。");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("承認処理に失敗しました: " + e.getMessage());
    }
  }

  /**
   * 管理者窓口3: 申請を差し戻す
   * POST http://localhost:8080/api/admin/attendance/reject
   */
  @PostMapping("/reject")
  public ResponseEntity<String> rejectRequest(@RequestBody Map<String, String> payload) {
    String recordId = payload.get("recordId");
    String comment = payload.get("adminComment");

    if (comment == null || comment.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("差戻し理由（コメント）を入力してください。");
    }

    try {
      adminService.rejectTimechange(recordId, comment);
      return ResponseEntity.ok("申請を差し戻しました。");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("差戻し処理に失敗しました: " + e.getMessage());
    }
  }
}