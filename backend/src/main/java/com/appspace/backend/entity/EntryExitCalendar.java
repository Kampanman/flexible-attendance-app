package com.appspace.backend.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "entry_exit_calendar") // 仕様書準拠のテーブル名
@Data
public class EntryExitCalendar {

  @Id
  @Column(name = "record_id", length = 50)
  private String recordId; // 登録日の yyyyMMddhhmmssss + UUID末尾4桁

  @Column(name = "record_date", nullable = false)
  private LocalDate recordDate; // カレンダーの「日付」 (yyyy-MM-dd)

  @Column(name = "registed_account_id", nullable = false)
  private String registedAccountId; // 申請ユーザーの accountId

  // --- 💡 確定・承認済みの実際の打刻時刻（Stringの hh:mm:ss 形式で扱うとフロントとの連携が劇的に楽になります） ---
  @Column(length = 10)
  private String entryTime; // 確定Entry時刻 (例: "09:00:00")

  @Column(length = 10)
  private String exitTime; // 確定Exit時刻 (例: "18:00:00")

  // --- 💡 編集申請中（管理者承認待ち）のテンポラリ時刻保存用フィールド ---
  @Column(length = 10)
  private String tmpEntryTime; // 申請中のEntry時刻案

  @Column(length = 10)
  private String tmpExitTime; // 申請中のExit時刻案

  // --- 💡 仕様書に記載された状態管理フラグとメッセージ ---
  @Column(name = "is_schedule_demand")
  private int isScheduleDemand = 0; // 予定申請中フラグ (0:なし, 1:申請中)

  @Column(name = "schedule_status")
  private int scheduleStatus = 0; // 予定申請ステータス (0:未承認, 1:差戻, 2:承認済)

  @Column(name = "is_timechange_demand")
  private int isTimechangeDemand = 0; // 時刻修正申請中フラグ (0:なし, 1:申請中)

  @Column(name = "timechange_status")
  private int timechangeStatus = 0; // 時刻修正ステータス (0:未承認, 1:差戻, 2:承認済)

  @Column(columnDefinition = "TEXT")
  private String reason; // ユーザーからの修正理由・備考

  @Column(columnDefinition = "TEXT")
  private String adminComment; // 管理者からの差戻理由・コメント
}
