package com.appspace.backend.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appspace.backend.dto.FuturePlanDTO;
import com.appspace.backend.entity.FuturePlan;
import com.appspace.backend.service.FuturePlanService;

@RestController
@RequestMapping("/api")
public class FuturePlanController {

  @Autowired
  private FuturePlanService futurePlanService;

  // =================================================================
  // 一般ユーザー向けエンドポイント群
  // =================================================================

  /**
   * 窓口1: 一般ユーザーからの新規予定申請の受付
   * POST http://localhost:8080/api/plans/request
   */
  @PostMapping("/plans/request")
  public ResponseEntity<String> requestFuturePlan(@RequestBody Map<String, Object> payload) {
    String accountId = (String) payload.get("accountId");
    String planDateStr = (String) payload.get("planDate"); // "yyyy-MM-dd"
    String planTitle = (String) payload.get("planTitle");
    String planDetail = (String) payload.get("planDetail");

    // 二重防壁バリデーション
    if (accountId == null || accountId.trim().isEmpty())
      return ResponseEntity.badRequest().body("【却下】アカウントIDが不正です。");
    if (planDateStr == null || planDateStr.trim().isEmpty())
      return ResponseEntity.badRequest().body("【却下】予定対象日を指定してください。");
    if (planTitle == null || planTitle.trim().isEmpty())
      return ResponseEntity.badRequest().body("【却下】予定の区分・タイトルを入力してください。");

    try {
      LocalDate planDate = LocalDate.parse(planDateStr);
      // 安全策: 過去日付への申請は弾く
      if (planDate.isBefore(LocalDate.now())) {
        return ResponseEntity.badRequest().body("【却下】過去の日付に対して新しく予定を申請することはできません。");
      }

      futurePlanService.createFuturePlan(accountId, planDate, planTitle, planDetail);
      return ResponseEntity.ok("予定申請を正常に提出しました。管理者の承認をお待ちください。");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("予定申請の処理中にエラーが発生しました: " + e.getMessage());
    }
  }

  /**
   * 窓口2: 一般ユーザー用：自身のダッシュボードに表示する「予定一覧」の取得
   * GET http://localhost:8080/api/plans/my-list?accountId=xxx
   */
  @GetMapping("/plans/my-list")
  public ResponseEntity<?> getMyPlans(@RequestParam String accountId) {
    if (accountId == null || accountId.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("アカウントIDが指定されていません。");
    }
    try {
      List<FuturePlan> myList = futurePlanService.getPlansByAccount(accountId);
      return ResponseEntity.ok(myList);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("予定一覧の取得に失敗しました。");
    }
  }

  /**
   * 【重要仕様】窓口3: ログイン時呼び出し専用：過去の予定を自動削除するトリガー
   * POST http://localhost:8080/api/plans/sync-login
   */
  @PostMapping("/plans/sync-login")
  public ResponseEntity<String> syncLoginCleanPastPlans(@RequestBody Map<String, String> payload) {
    String accountId = payload.get("accountId");

    if (accountId == null || accountId.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("同期エラー: アカウントIDが不明です。");
    }
    try {
      // 過去の予定データを物理消去
      futurePlanService.cleanPastPlans(accountId);
      return ResponseEntity.ok("過去日の予定データのリフレッシュに成功しました。");
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("ログイン時の予定データ自動クリーンアップ中にエラーが発生しました。");
    }
  }

  // =================================================================
  // 管理者ユーザー（Admin）向けエンドポイント群
  // =================================================================

  /**
   * 窓口4: 管理者用：現在届いている「未承認の予定申請」をすべて一覧取得する
   * GET http://localhost:8080/api/admin/plans/pending-list
   */
  @GetMapping("/admin/plans/pending-list")
  public ResponseEntity<List<FuturePlanDTO>> getAdminPendingPlans() {
    try {
      // Service側でDTOに変換された綺麗なリストがそのまま返却されます
      List<FuturePlanDTO> dtoList = futurePlanService.getPendingPlansForAdmin();
      return ResponseEntity.ok(dtoList);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(null);
    }
  }

  /**
   * 窓口5: 管理者用：届いた申請を「承認」または「差戻し」する
   * POST http://localhost:8080/api/admin/plans/judge
   */
  @PostMapping("/admin/plans/judge")
  public ResponseEntity<String> judgeUserPlan(@RequestBody Map<String, Object> payload) {
    String planId = (String) payload.get("planId");
    Integer targetStatus = (Integer) payload.get("targetStatus"); // 1:差戻し, 2:承認
    String adminComment = (String) payload.get("adminComment");

    if (planId == null || planId.trim().isEmpty())
      return ResponseEntity.badRequest().body("【却下】対象の申請IDが指定されていません。");
    if (targetStatus == null || (targetStatus != 1 && targetStatus != 2))
      return ResponseEntity.badRequest().body("【却下】判定ステータスが不正です。");

    try {
      futurePlanService.judgePlan(planId, targetStatus, adminComment);
      String message = (targetStatus == 2) ? "予定申請を「承認」しました。" : "予定申請を「差戻し」しました。";
      return ResponseEntity.ok(message);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("判定処理中にエラーが発生しました: " + e.getMessage());
    }
  }
}