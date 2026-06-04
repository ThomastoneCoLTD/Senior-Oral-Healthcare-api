package com.kaii.dentix.global.config;

import com.kaii.dentix.domain.jwt.JwtAuthenticationFilter;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /* =============================
                 * 보안 헤더 (ZAP 대응 핵심)
                 * ============================= */
                .headers(headers -> headers
                        // CSP (XSS / Clickjacking 방어)
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data: blob: " +
                                                "https://denti-dev.s3.ap-southeast-1.amazonaws.com " +
                                                "https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com; " +
                                                "font-src 'self'; " +
                                                "connect-src 'self' " +
                                                "https://denti-dev.s3.ap-southeast-1.amazonaws.com " +
                                                "https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com; " +
                                                "media-src 'self' " +
                                                "https://denti-dev.s3.ap-southeast-1.amazonaws.com " +
                                                "https://tms-static-hosting.s3.ap-northeast-2.amazonaws.com; " +
                                                "object-src 'none'; " +
                                                "frame-ancestors 'none';"
                                )
                        )

                        // 클릭재킹 방어
                        .frameOptions(frame -> frame.deny())

                        // MIME Sniffing 방지
                        .contentTypeOptions(Customizer.withDefaults())

                        // HTTPS 강제 (HSTS)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )

                        // Referrer 최소화
                        .referrerPolicy(referrer ->
                                referrer.policy(
                                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                                )
                        )
                )

                /* =============================
                 * 인증 / 인가
                 * ============================= */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/oral-exercise").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/admin/billing/export/excel/**",
                                "/admin/user/bulk-upload/template/**"
                        ).permitAll()
                        .requestMatchers(EXCLUDE_URLS).permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://denti.thomabio.com",
                "https://*.thomabio.com"
        ));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
