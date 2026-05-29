package com.appspace.backend.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appspace.backend.entity.Announcement;
import com.appspace.backend.repository.AnnouncementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

  @Autowired
  private Logger logger;

  private final AnnouncementRepository announcementRepository;

  /**
   * ロジック1: お知らせ一覧を全件取得する
   */
  public List<Announcement> getAllAnnouncements() {
    return announcementRepository.findAll();
  }

  /**
   * ロジック2: お知らせの【新規登録】および【編集】を保存する
   * 
   * @param id 新規の場合は null または空文字、編集の場合は対象の UUID
   */
  public Announcement saveOrUpdateAnnouncement(String id, String title, String about, Integer isDeletable) {
    Announcement announcement;

    // IDの有無によって「新規登録」か「上書き編集」かをジャッジします
    if (id != null && !id.trim().isEmpty()) {
      // 【編集の場合】既存レコードをリポジトリから掘り起こす
      announcement = announcementRepository.findById(id)
          .orElseThrow(() -> new RuntimeException("指定されたお知らせが見つかりません。ID: " + id));
    } else {
      // 【新規登録の場合】新しくインスタンスを立ち上げ、UUIDを払い出す
      announcement = new Announcement();
      announcement.setAnnouncementId(UUID.randomUUID().toString());
    }

    // 既存フィールド名に綺麗にマッピングして格納
    announcement.setAnnouncementTitle(title.trim());
    announcement.setAnnouncementAbout(about.trim());

    // データベースへ保存（JPAがINSERTかUPDATEかを自動で判別します）
    Announcement savedAnnouncement = announcementRepository.save(announcement);
    logger.info("=== [Service] お知らせを保存しました (ID: {}) ===", savedAnnouncement.getAnnouncementId());

    return savedAnnouncement;
  }

  /**
   * ロジック3: 指定されたお知らせをデータベースから完全に削除する
   * 
   * @param id 削除対象のお知らせID (UUID)
   */
  public void deleteAnnouncement(String id) {
    // 安全ガード：対象のデータが本当に存在するかチェック
    if (!announcementRepository.existsById(id)) {
      throw new RuntimeException("削除しようとしたお知らせは見つかりませんでした。ID: " + id);
    }

    // データの削除を実行
    announcementRepository.deleteById(id);
    logger.info("=== [Service] お知らせをデータベースから削除しました (ID: {}) ===", id);
  }
}