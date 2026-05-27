package com.appspace.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appspace.backend.entity.EntryExitCalendar;
import com.appspace.backend.repository.EntryExitCalendarRepository;

@Service
@Transactional
public class AdminAttendanceService {

  @Autowired
  private Logger logger;

  @Autowired
  private EntryExitCalendarRepository calendarRepository;

  /**
   * 管理者ロジック1: 現在申請が届いているレコードを全件取得する
   */
  public List<EntryExitCalendar> getPendingTimechangeRequests() {
    return calendarRepository.findByIsTimechangeDemandOrderByRecordDateAsc(1); // 「1：申請中」のものを利口に全件取得
  }

  /**
   * 管理者ロジック2: 申請を【承認】する
   * 
   * @param recordId 対象のレコードID (RECxxxx)
   */
  public void approveTimechange(String recordId) {
    EntryExitCalendar record = calendarRepository.findById(recordId)
        .orElseThrow(() -> new RuntimeException("指定された申請レコードが見つかりません。"));

    // データの引越し：仮保存されていた希望時刻を、本番の確定時刻枠にコピー！
    record.setEntryTime(record.getTmpEntryTime());
    record.setExitTime(record.getTmpExitTime());

    // ステータスを「2：承認済み」にする
    record.setTimechangeStatus(2);
    // 処理が完了したので、申請中フラグを「0（通常状態）」に下げる
    record.setIsTimechangeDemand(0);

    calendarRepository.save(record); // データベースを確定更新！
    logger.info("=== [Admin Logic] 申請の承認および時刻の確定上書きが完了しました ===");
  }

  /**
   * 管理者ロジック3: 申請を【差戻し（却下）】する
   * 
   * @param recordId     対象のレコードID
   * @param adminComment 管理者からの言い分・理由
   */
  public void rejectTimechange(String recordId, String adminComment) {
    EntryExitCalendar record = calendarRepository.findById(recordId)
        .orElseThrow(() -> new RuntimeException("指定された申請レコードが見つかりません。"));

    // ステータスを「1：差戻し」にする
    record.setTimechangeStatus(1);
    // ユーザーが再修正できるように、申請中フラグは「1（申請状態）」のままキープ、または仕様に合わせて調整
    // 今回はユーザー画面側の「再申請」ボタン活性化のために1のまま、あるいは状態を維持します
    record.setAdminComment(adminComment); // 差戻し理由を格納

    calendarRepository.save(record);
    logger.info("=== [Admin Logic] 申請の差戻し処理が完了しました ===");
  }
}