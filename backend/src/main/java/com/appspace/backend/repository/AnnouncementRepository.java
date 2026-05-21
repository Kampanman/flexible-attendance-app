package com.appspace.backend.repository;

import com.appspace.backend.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, String> {
    // 最新のお知らせをすべて取得（必要に応じて並び替えメソッドにしてもOK）
    List<Announcement> findAll();
}