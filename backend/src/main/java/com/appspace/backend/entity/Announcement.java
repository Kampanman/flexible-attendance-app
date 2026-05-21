package com.appspace.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "announcement")
@Data
public class Announcement {
    @Id
    private String announcementId; // UUID
    private String announcementTitle;
    private String announcementAbout;
    private Integer isDeletable; // 0:削除不可, 1:削除可能
}