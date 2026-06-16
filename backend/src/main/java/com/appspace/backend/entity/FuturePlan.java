package com.appspace.backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "future_plan")
@Data
public class FuturePlan {

  @Id
  private String planId; // 一意の識別子（UUIDを生成して格納）

  private String accountId; // 申請したユーザーのID

  private LocalDate planDate; // 予定対象日（例: 2026-06-15）

  private String planTitle; // 予定の区分・タイトル（例: 「有給休暇」「出張」「在宅勤務」など）

  private String planDetail; // 理由や備考（例: 「私用のため」「〇〇社訪問のため」）

  /**
   * 承認ステータス
   * 0: 申請中 (未承認)
   * 1: 差戻し (却下)
   * 2: 承認済み
   */
  private Integer planStatus;

  private String adminComment; // 差戻し・却下時に管理者が入力するコメント（空欄も許容）

  private LocalDateTime createdAt; // 申請日時（自動クリーンアップの補助やログ用）
}