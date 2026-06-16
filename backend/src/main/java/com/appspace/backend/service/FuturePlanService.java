package com.appspace.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appspace.backend.dto.FuturePlanDTO;
import com.appspace.backend.entity.FuturePlan;
import com.appspace.backend.repository.FuturePlanRepository;
import com.appspace.backend.repository.UserAccountRepository;

@Service
public class FuturePlanService {

  @Autowired
  private Logger logger;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private FuturePlanRepository futurePlanRepository;

  /**
   * ロジック1: 一般ユーザーからの新しい予定申請をデータベースに登録する
   */
  public void createFuturePlan(String accountId, LocalDate planDate, String planTitle, String planDetail) {
    FuturePlan plan = new FuturePlan();
    plan.setPlanId(UUID.randomUUID().toString()); // 新しいUUIDを発行
    plan.setAccountId(accountId);
    plan.setPlanDate(planDate);
    plan.setPlanTitle(planTitle.trim());
    plan.setPlanDetail(planDetail != null ? planDetail.trim() : "");
    plan.setPlanStatus(0); // 0: 申請中 (未承認) で初期化
    plan.setAdminComment("");
    plan.setCreatedAt(LocalDateTime.now());

    futurePlanRepository.save(plan);
    logger.info("=== [Service] 予定申請を登録しました (ID: {}) ===", plan.getPlanId());
  }

  /**
   * ロジック2: 特定の一般ユーザーの予定一覧を取得する
   */
  public List<FuturePlan> getPlansByAccount(String accountId) {
    return futurePlanRepository.findByAccountIdOrderByPlanDateAsc(accountId);
  }

  /**
   * ロジック3: 管理者画面用：すべての「申請中 (0)」の予定を一覧取得する
   */
  public List<FuturePlanDTO> getPendingPlansForAdmin() {
    // 1. まずは通常通りステータス0の生の予定リストを取得
    List<FuturePlan> rawPlans = futurePlanRepository.findByPlanStatusOrderByPlanDateAsc(0);

    // 2. Stream API を使って、1件ずつ名前をドッキングしてDTOへ詰め替える
    return rawPlans.stream().map(plan -> {
      FuturePlanDTO dto = new FuturePlanDTO();
      dto.setPlanId(plan.getPlanId());
      dto.setAccountId(plan.getAccountId());
      dto.setPlanDate(plan.getPlanDate());
      dto.setPlanTitle(plan.getPlanTitle());
      dto.setPlanDetail(plan.getPlanDetail());
      dto.setPlanStatus(plan.getPlanStatus());
      dto.setAdminComment(plan.getAdminComment());
      dto.setCreatedAt(plan.getCreatedAt());

      // ユーザーアカウントテーブルから名前を引き去り、ドッキングする
      dto.setUserName("不明な社員");
      try {
        String targetId = plan.getAccountId();

        userAccountRepository.findByAccountId(targetId).ifPresent(acc -> {
          dto.setUserName(acc.getUserName());
        });
      } catch (Exception e) {
        // セーフティ
      }

      return dto;
    }).collect(java.util.stream.Collectors.toList());
  }

  /**
   * ロジック4: 管理者による承認、または却下（差戻し）の裁定を反映する
   */
  public void judgePlan(String planId, Integer targetStatus, String adminComment) {
    FuturePlan plan = futurePlanRepository.findById(planId)
        .orElseThrow(() -> new RuntimeException("指定された予定申請が見つかりませんでした。ID: " + planId));

    // ステータスを更新 (1:差戻し, 2:承認済み)
    plan.setPlanStatus(targetStatus);
    plan.setAdminComment(adminComment != null ? adminComment.trim() : "");

    futurePlanRepository.save(plan);
    logger.info("=== [Service] 予定の判定を更新しました (ID: {}, Status: {}) ===", planId, targetStatus);
  }

  /**
   * 【重要仕様】ログイン時用：操作日より過去の日付となった予定を自動で物理削除する
   */
  @Transactional
  public void cleanPastPlans(String accountId) {
    LocalDate today = LocalDate.now();

    // Repositoryで定義した独自クエリの呼び出し
    futurePlanRepository.deleteByAccountIdAndPlanDateBefore(accountId, today);
    logger.info("=== [Service] アカウントID: {} の本日の操作日 ({}) より古い過去の予定を自動削除しました ===", accountId, today);
  }
}
