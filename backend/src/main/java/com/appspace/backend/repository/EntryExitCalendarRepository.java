package com.appspace.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.appspace.backend.entity.EntryExitCalendar;

@Repository
public interface EntryExitCalendarRepository extends JpaRepository<EntryExitCalendar, String> {

  /**
   * 特定ユーザーの、指定された期間（1か月分など）のレコードを日付順にすべて取得する
   */
  List<EntryExitCalendar> findByRegistedAccountIdAndRecordDateBetweenOrderByRecordDateAsc(
      String accountId, LocalDate startDate, LocalDate endDate);

  /**
   * 特定ユーザーの、特定の日付のレコードを1件取得する
   */
  Optional<EntryExitCalendar> findByRegistedAccountIdAndRecordDate(String accountId, LocalDate recordDate);

  // 管理者用：日本全国の全ユーザーの中から、「時刻修正申請中（isTimechangeDemand = 1）」の行だけを全て取ってくる命令
  List<EntryExitCalendar> findByIsTimechangeDemandOrderByRecordDateAsc(int isTimechangeDemand);
}