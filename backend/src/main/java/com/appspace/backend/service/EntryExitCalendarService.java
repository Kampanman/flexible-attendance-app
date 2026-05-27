package com.appspace.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appspace.backend.entity.EntryExitCalendar;
import com.appspace.backend.repository.EntryExitCalendarRepository;

@Service
public class EntryExitCalendarService {

  @Autowired
  private Logger logger;

  @Autowired
  private EntryExitCalendarRepository calendarRepository; // 先ほど作った翻訳窓口（リポジトリ）を接続

  /**
   * 業務ロジック1: 指定された年月の1か月分のリストカレンダーを取得・生成する
   * 
   * @param accountId    対象ユーザーのUUID
   * @param yearMonthStr 対象年月 (例: "2026-05")
   */
  public List<EntryExitCalendar> getMonthlyCalendar(String accountId, String yearMonthStr) {
    // 1. "2026-05" という文字列を、Javaが計算できる YearMonth 型に変換します
    YearMonth targetMonth = YearMonth.parse(yearMonthStr);

    // 2. その月の「1日」と「末日」を割り出します
    LocalDate startDate = targetMonth.atDay(1);
    LocalDate endDate = targetMonth.atEndOfMonth();

    // 3. リポジトリ窓口を使って、すでにDBに登録されている該当期間のレコードを日付順にシュッと取得します
    List<EntryExitCalendar> existingRecords = calendarRepository
        .findByRegistedAccountIdAndRecordDateBetweenOrderByRecordDateAsc(accountId, startDate, endDate);

    // 4. 【ここが職人技】1日から末日までの「完璧な1か月分のリスト」を入れる器を用意します
    List<EntryExitCalendar> fullMonthCalendar = new ArrayList<>();

    // 5. 1日から末日 まで1日ずつループを回します
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      final LocalDate currentLoopDate = date; // ループ内の日付を固定化

      // すでにDBにレコードが存在するかチェックします
      Optional<EntryExitCalendar> foundRecord = existingRecords.stream()
          .filter(r -> r.getRecordDate().equals(currentLoopDate))
          .findFirst();

      if (foundRecord.isPresent()) {
        // すでにデータが存在すれば、それをそのままリストに採用！
        fullMonthCalendar.add(foundRecord.get());
      } else {
        // 💡 まだ打刻データがない日（未出勤の日や未来の日）の場合、
        // 画面で「空行」として表示できるように、その場で見映え用の空レコードを偽造してあげます
        EntryExitCalendar blankDay = new EntryExitCalendar();
        blankDay.setRecordDate(currentLoopDate);
        blankDay.setRegistedAccountId(accountId);
        blankDay.setEntryTime("-"); // 時刻はまだないのでハイフン
        blankDay.setExitTime("-");
        // 各種フラグやステータスも初期状態（0）のままセット
        fullMonthCalendar.add(blankDay);
      }
    }

    return fullMonthCalendar; // 1日〜末日まで1行の漏れもない完璧な1か月分データを返却！
  }

  /**
   * 業務ロジック2: ユーザーからの打刻修正（編集）申請を処理する
   */
  @Transactional // データの書き換えを安全に行うための宣言（エラーが起きたら自動で巻き戻す）
  public void requestTimeChange(String accountId, LocalDate recordDate, String tmpEntry, String tmpExit,
      String reason) {

    // 1. まず、その日のレコードが既にDBにあるかどうかをピンポイント検索します
    Optional<EntryExitCalendar> existingOpt = calendarRepository
        .findByRegistedAccountIdAndRecordDate(accountId, recordDate);

    EntryExitCalendar targetRow;

    if (existingOpt.isPresent()) {
      // すでに打刻データがある日なら、その既存データを書き換え対象にします
      targetRow = existingOpt.get();
    } else {
      // 丸ごと押し忘れてデータが1件もない日の場合、新しく行を立ち上げます
      targetRow = new EntryExitCalendar();

      // 仕様書通りのルール（登録日の yyyyMMddhhmmssss + UUID末尾4桁）で record_id を生成
      String timePart = java.time.LocalDateTime.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String uuidPart = UUID.randomUUID().toString().substring(32); // 末尾4桁を取得
      targetRow.setRecordId("REC" + timePart + uuidPart);

      targetRow.setRecordDate(recordDate);
      targetRow.setRegistedAccountId(accountId);
      targetRow.setEntryTime("-"); // 元の打刻時刻は空なのでハイフン
      targetRow.setExitTime("-");
    }

    // 2. 仕様書に指定された「申請中ステータス」の値をパチパチとセットします
    targetRow.setTmpEntryTime(tmpEntry); // ユーザーが希望する新しいEntry時刻
    targetRow.setTmpExitTime(tmpExit); // ユーザーが希望する新しいExit時刻
    targetRow.setIsTimechangeDemand(1); // ★時刻修正申請中フラグを「1:申請中」にする
    targetRow.setTimechangeStatus(0); // ★ステータスを「0:未承認」にする
    targetRow.setReason(reason); // 修正の理由（備考）を格納
    targetRow.setAdminComment(""); // 申請し直しに備えて管理者の過去コメントをクリア

    // 3. 翻訳窓口（リポジトリ）に「これで保存して！」と手渡します
    calendarRepository.save(targetRow);
    logger.info("=== [Service] DBへの申請ステータス書き込みが正常完了しました ===");
  }
}