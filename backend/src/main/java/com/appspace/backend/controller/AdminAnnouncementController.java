package com.appspace.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.entity.Announcement;
import com.appspace.backend.service.AnnouncementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://*.app.github.dev")
public class AdminAnnouncementController {

  private final AnnouncementService announcementService;

  /**
   * 窓口1: 管理者用に現在登録されているお知らせ一覧を全件取得する
   * GET http://localhost:8080/api/admin/announcements
   */
  @GetMapping
  public ResponseEntity<List<Announcement>> getAllAnnouncements() {
    return ResponseEntity.ok(announcementService.getAllAnnouncements());
  }

  /**
   * 窓口2: お知らせの新規登録・上書き編集リクエストをまとめて受け付ける
   * POST http://localhost:8080/api/admin/announcements/save
   */
  @PostMapping("/save")
  public ResponseEntity<String> saveAnnouncement(@RequestBody Map<String, Object> payload) {
    // フィールド名に適合させてリクエストを取り出し
    String id = (String) payload.get("announcementId"); // 新規のときは null または ""
    String title = (String) payload.get("announcementTitle");
    String about = (String) payload.get("announcementAbout");

    // バックエンド側での不正データブロック（二重防壁バリデーション）
    if (title == null || title.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("【却下】お知らせのタイトルを入力してください。");
    }
    if (about == null || about.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("【却下】お知らせの本文・詳細を入力してください。");
    }

    try {
      // サービス層の保存・編集ロジックをキック
      announcementService.saveOrUpdateAnnouncement(id, title, about, 1);
      return ResponseEntity.ok("お知らせの内容を正常に反映・保存しました。");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("お知らせの保存処理中にエラーが発生しました: " + e.getMessage());
    }
  }

  /**
   * 窓口3: 指定されたお知らせの削除リクエストを受け付ける
   * DELETE http://localhost:8080/api/admin/announcements/delete/{announcementId}
   */
  @DeleteMapping("/delete/{id}")
  public ResponseEntity<String> deleteAnnouncement(@PathVariable String id) {
    // バックエンド側での簡易ガード（ID空チェック）
    if (id == null || id.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("【却下】不正な呼び出しです。削除対象のIDが指定されていません。");
    }

    try {
      // サービス層の削除処理を呼び出す
      announcementService.deleteAnnouncement(id);
      return ResponseEntity.ok("お知らせを正常に削除しました。");
    } catch (RuntimeException e) {
      // Service側でデータが見つからなかった場合の個別エラー返却
      return ResponseEntity.status(404).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("お知らせの削除処理中にエラーが発生しました: " + e.getMessage());
    }
  }
}