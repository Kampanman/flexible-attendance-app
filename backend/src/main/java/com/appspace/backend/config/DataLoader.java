package com.appspace.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.appspace.backend.service.UserAccountService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

  private final UserAccountService userService;

  @Override
  public void run(String... args) throws Exception {
    // アプリ起動時に、管理者アカウントの自動生成ロジックを実行
    userService.initializeAdminAccount();
  }
}