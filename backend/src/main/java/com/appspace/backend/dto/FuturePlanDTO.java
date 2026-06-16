package com.appspace.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FuturePlanDTO {
  private String planId;
  private String accountId;
  private String userName; // これが画面に表示される待望のユーザー名枠です！
  private LocalDate planDate;
  private String planTitle;
  private String planDetail;
  private Integer planStatus;
  private String adminComment;
  private LocalDateTime createdAt;
}