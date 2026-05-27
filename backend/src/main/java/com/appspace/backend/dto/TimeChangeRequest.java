package com.appspace.backend.dto;

import java.time.LocalDate;

public class TimeChangeRequest {

  private String accountId;
  private LocalDate recordDate; // 修正対象の日付 (yyyy-MM-dd)
  private String targetEntryTime; // 修正希望Entry時刻 (hh:mm:ss)
  private String targetExitTime; // 修正希望Exit時刻 (hh:mm:ss)
  private String reason; // 編集申請の備考・理由
  // ゲッター、セッター

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public LocalDate getRecordDate() {
    return recordDate;
  }

  public void setRecordDate(LocalDate recordDate) {
    this.recordDate = recordDate;
  }

  public String getTargetEntryTime() {
    return targetEntryTime;
  }

  public void setTargetEntryTime(String targetEntryTime) {
    this.targetEntryTime = targetEntryTime;
  }

  public String getTargetExitTime() {
    return targetExitTime;
  }

  public void setTargetExitTime(String targetExitTime) {
    this.targetExitTime = targetExitTime;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}