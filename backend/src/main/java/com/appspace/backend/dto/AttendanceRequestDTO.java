package com.appspace.backend.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AttendanceRequestDTO {
  private String recordId;
  private String accountId;
  private String userName; // 新設：結合したユーザー名を入れる特等席
  private LocalDate recordDate;
  private String entryTime;
  private String exitTime;
  private String tmpEntryTime;
  private String tmpExitTime;
  private String reason;
  private String adminComment;
}