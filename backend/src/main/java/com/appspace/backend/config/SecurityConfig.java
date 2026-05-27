package com.appspace.backend.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * Beanアノテーションを一言でいうなら、Springという工場に部品（オブジェクト）の作り方を教えて、工場の管理リストに登録してもらうための印。
     * SpringBootでは「インスタンスの作成や管理を丸投げする」という手法（DI：依存性の注入）が可能であり、いちいち「new
     * MyClass()」とする必要がない。
     * Beanアノテーションを用いることで、Spring標準の動きを上書きしたり、独自のルールを登録することが出来るのである。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS設定を有効化
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/h2-console/**",
                                "/api/users/**",
                                "/api/system/**",
                                "/api/attendance/**",
                                "/api/attendance/history/**",
                                "/api/admin/attendance/**",
                                "/api/dashboard/**")
                        .permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORSの具体的な許可ルールを定義するメソッド
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // フロントエンドのURLを許可（ワイルドカード "*" を使うか、正確なURLを指定します）
        // Codespaces環境では "*" を指定するのが最も確実です
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 許可するメソッド（GET, POSTなど）
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 許可するヘッダー
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));

        // クッキーなどの認証情報を許可するか（今回はfalseでOK）
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 全パスに適用
        return source;
    }

    // LoggerをあらゆるControllerやServiceなどのクラスで利用できるようにする
    @Bean
    @Scope("prototype") // 注入先ごとにインスタンスを生成するために必須
    public Logger logger(InjectionPoint injectionPoint) {
        // 注入先のクラス名を取得してLoggerを生成
        return LoggerFactory.getLogger(
                injectionPoint.getMethodParameter() != null
                        ? injectionPoint.getMethodParameter().getContainingClass()
                        : injectionPoint.getField().getDeclaringClass());
    }
}