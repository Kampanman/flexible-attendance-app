package com.appspace.backend.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "https://*.app.github.dev") // 環境に合わせて調整してください
public class SystemConfigController {

  @Autowired
  private Logger logger;

  // 初期状態は 「0: 勤怠モード」 に設定
  private int currentAttendanceMode = 0;

  /**
   * 現在の打刻モードを取得する（全ユーザーが利用）
   * GET http://localhost:8080/api/system/mode
   */
  @GetMapping("/mode")
  public ResponseEntity<Map<String, Object>> getAttendanceMode() {
    // Vue側で扱いやすいように JSON オブジェクトの形（{ "mode": 0 }）で返却
    return ResponseEntity.ok(Map.of("mode", currentAttendanceMode));
  }

  /**
   * 統括管理者が打刻モードを一括変更するエンドポイント
   * PUT http://localhost:8080/api/system/mode
   */
  @PutMapping("/mode")
  public ResponseEntity<String> updateAttendanceMode(@RequestBody Map<String, Integer> requestBody) {
    try {
      if (!requestBody.containsKey("mode")) {
        return ResponseEntity.badRequest().body("モード値が指定されていません。");
      }

      int newMode = requestBody.get("mode");

      // 0, 1, 2 以外の不正な数値はバリデーションで弾く
      if (newMode < 0 || newMode > 2) {
        return ResponseEntity.badRequest().body("無効なモード値です。");
      }

      // モードを更新
      this.currentAttendanceMode = newMode;
      logger.info("=== [System] 統括管理者によって打刻モードが変更されました: {} ===", currentAttendanceMode);

      return ResponseEntity.ok("打刻モードを更新しました。");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("更新失敗: " + e.getMessage());
    }
  }
}