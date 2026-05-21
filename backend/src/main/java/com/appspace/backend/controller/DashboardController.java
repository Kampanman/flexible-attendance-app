package com.appspace.backend.controller;

import com.appspace.backend.entity.Announcement;
import com.appspace.backend.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*") // 環境に合わせて調整
public class DashboardController {

    @Autowired
    private AnnouncementRepository announcementRepository;

    /**
     * ダッシュボード用のお知らせ一覧を取得する
     * GET /api/dashboard/announcements
     */
    @GetMapping("/announcements")
    public ResponseEntity<List<Announcement>> getAnnouncements() {
        List<Announcement> list = announcementRepository.findAll();
        return ResponseEntity.ok(list);
    }
}