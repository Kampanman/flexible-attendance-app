package com.appspace.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appspace.backend.entity.FuturePlan;

public interface FuturePlanRepository extends JpaRepository<FuturePlan, String> {

  // 1. 特定の一般ユーザーの予定を、対象日順（昇順）に並べてすべて取得
  List<FuturePlan> findByAccountIdOrderByPlanDateAsc(String accountId);

  // 2. 管理者画面用：すべてのユーザーの「申請中 (status = 0)」の予定を古い順に取得
  List<FuturePlan> findByPlanStatusOrderByPlanDateAsc(Integer planStatus);

  // 3. 【重要仕様】操作日より前の日付となった過去の予定を物理削除する（@Queryを使わない、JPA標準ルールに則った自動削除メソッド）
  // メソッド名だけで「accountId が一致、かつ planDate が指定日より前（Before）のものを削除する」という意味
  void deleteByAccountIdAndPlanDateBefore(String accountId, LocalDate today);
}