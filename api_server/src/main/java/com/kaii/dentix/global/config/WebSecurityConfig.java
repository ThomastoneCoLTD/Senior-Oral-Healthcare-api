package com.kaii.dentix.global.config;

import com.kaii.dentix.domain.jwt.JwtAuthenticationFilter;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtTokenUtil jwtTokenUtil;

    public static String[] EXCLUDE_URLS = {
            "/actuator/health",
            "/docs/*",
            "/login", "/login/*",
            "/admin/user/bulk-upload/template/*",
            "/password", "/password/*",
            "/service-agreement",
            "/contents", "/contents/*",
            "/login/password/*",
            "/organizations/check/**",
            "/admin/login",
            "/admin/register", "/admin/register/*",
            "/admin/account", "/admin/account/*",
            "/admin/password","/admin/find-password",
            "/admin/auto-login"
    };

    @Bean
    public PasswordEncoder PasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // [수정 1] 보안 헤더 설정 (Native DSL 사용)
                .headers(headers -> headers
                        // 1. X-Frame-Options: Clickjacking 방어 (SAMEORIGIN 권장, 기존 DENY 유지 시 프레임 사용 불가)
                        .frameOptions(frame -> frame.sameOrigin())

                        // 2. XSS Protection: 브라우저 XSS 필터 활성화
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

                        // 3. CSP (Content Security Policy): 스크립트 실행 출처 제한
                        // 보고서에 언급된 도메인(*.denti.thomabio.com)을 허용 목록에 추가
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' *.denti.thomabio.com; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "object-src 'none'; " +
                                        "base-uri 'self';")
                        )

                        // 4. HSTS (HTTP Strict Transport Security): HTTPS 강제 (보고서 62번 항목 해결)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000) // 1년
                        )

                        // 5. Cache Control: 민감 정보 캐싱 방지 (기본값: no-cache, no-store, max-age=0, must-revalidate)
                        .cacheControl(cache -> {})
                )

                .authorizeHttpRequests(auth -> auth
                        // CORS Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 인증 제외 URL (여기에 /login 추가 필수)
                        .requestMatchers(
                                "/login",                               // [추가] 로그인 경로는 누구나 접근 가능해야 함
                                "/admin/billing/export/excel/**",
                                "/admin/user/bulk-upload/template/**",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        // 기존 EXCLUDE_URLS에 /login이 없다면 위처럼 따로 적어주거나, 배열에 추가해야 합니다.
                        .requestMatchers(EXCLUDE_URLS).permitAll()

                        // 관리자 API
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // 일반 API - 허용 메소드만 인증 요구
                        // (로그인 /login 은 위에서 통과되었으므로 여기 검사에 걸리지 않게 됩니다)
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/**").authenticated()

                        // 불필요한 HTTP 메소드 차단
                        .anyRequest().denyAll()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class);

        // 디버깅용 로그 (필요 시 유지)
        // log.error("AUTH = {}", SecurityContextHolder.getContext().getAuthentication());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "https://denti.thomabio.com" // 운영 도메인 명시
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}