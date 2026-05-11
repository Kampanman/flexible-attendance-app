package com.appspace.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * Beanアノテーションを一言でいうなら、Springという工場に部品（オブジェクト）の作り方を教えて、工場の管理リストに登録してもらうための印。
     * SpringBootでは「インスタンスの作成や管理を丸投げする」という手法（DI：依存性の注入）が可能であり、いちいち「new MyClass()」とする必要がない。
     * Beanアノテーションを用いることで、Spring標準の動きを上書きしたり、独自のルールを登録することが出来るのである。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // テスト用なのでCSRF保護を無効化
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/register", "/api/users/login", "/api/attendance/**").permitAll() // /attendance/** で配下をすべて許可
                .anyRequest().authenticated()
            );
        return http.build();
    }
}