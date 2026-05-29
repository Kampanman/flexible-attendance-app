package com.appspace.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // 4. 1日から末日までの「完璧な1か月分のリスト」を入れる器を用意します
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
        // まだ打刻データがない日（未出勤の日や未来の日）の場合、
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
  @Transactional
  public void requestTimeChange(String accountId, java.time.LocalDate recordDate, String tmpEntry, String tmpExit,
      String reason) {

    // 1. その日のレコードが既にDBにあるかどうかを検索
    Optional<EntryExitCalendar> existingOpt = calendarRepository
        .findByRegistedAccountIdAndRecordDate(accountId, recordDate);

    EntryExitCalendar targetRow;

    if (existingOpt.isPresent()) {
      targetRow = existingOpt.get();
    } else {
      // データが1件もない日の場合、新しく行を立ち上げる
      targetRow = new EntryExitCalendar();
      String timePart = java.time.LocalDateTime.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
      String uuidPart = java.util.UUID.randomUUID().toString().substring(32);
      targetRow.setRecordId("REC" + timePart + uuidPart);

      targetRow.setRecordDate(recordDate);
      targetRow.setRegistedAccountId(accountId);
      targetRow.setEntryTime("-");
      targetRow.setExitTime("-");
    }

    // 2. 申請用の仮保存枠（tmp_xxx）に値をセット
    targetRow.setTmpEntryTime(tmpEntry);

    // 調整：もし当日の申請で、退勤がまだおこなわれておらず、
    // ユーザーもフォームの退勤欄を空（ハイフン）のまま申請してきた場合は、
    // 既存の確定データ（無ければハイフン）の状態を優しく維持して仮保存します
    if ("-".equals(tmpExit) && !"-".equals(targetRow.getExitTime())) {
      targetRow.setTmpExitTime(targetRow.getExitTime()); // 既に打刻済みの退勤時間があればそれをキープ
    } else {
      targetRow.setTmpExitTime(tmpExit); // 入力された値（新しい時間、またはハイフン）をそのままセット
    }

    // 3. 各種ステータスを「申請中」にセット
    targetRow.setIsTimechangeDemand(1); // 時刻修正申請中フラグを「1:申請中」
    targetRow.setTimechangeStatus(0); // ステータスを「0:未承認」
    targetRow.setReason(reason); // 理由を格納
    targetRow.setAdminComment(""); // 過去の管理者コメントをクリア

    // 4. データベースへ保存
    calendarRepository.save(targetRow);
    System.out.println("=== [Service] DBへの申請ステータス書き込みが正常完了しました ===");
  }

  /**
   * ユーザー自身による編集申請の取り消し処理
   */
  @Transactional
  public void cancelTimeChangeRequest(String accountId, LocalDate recordDate) {
    Optional<EntryExitCalendar> opt = calendarRepository.findByRegistedAccountIdAndRecordDate(accountId, recordDate);

    if (opt.isPresent()) {
      EntryExitCalendar record = opt.get();
      // 申請中（未承認）の場合のみ取り消しを許可する安全ガード
      if (record.getIsTimechangeDemand() == 1 && record.getTimechangeStatus() == 0) {
        record.setIsTimechangeDemand(0); // フラグを通常に戻す
        record.setTmpEntryTime(null); // 申請中だった仮時刻をクリア
        record.setTmpExitTime(null);
        record.setReason(null);

        calendarRepository.save(record);
        logger.info("=== [Service] ユーザーにより申請が取り消されました ===");
      }
    }
  }
}