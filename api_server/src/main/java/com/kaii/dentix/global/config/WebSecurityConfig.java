package com.kaii.dentix.global.config;

import com.kaii.dentix.domain.jwt.JwtAuthenticationFilter;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtTokenUtil jwtTokenUtil;

//    private final UserDeviceTypeService userDeviceTypeService;

    public static String[] EXCLUDE_URLS = {
            "/actuator/health",
            "/docs/*",
            "/login", "/login/*",
//            "/user", "/user/*",
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

    /**
     *  비밀번호 암호화
     */
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

                // ★ 여기서 응답 헤더 직접 추가 (XSS / MIME / CSP / Frame 방지)
                .headers(headers -> headers
                        .addHeaderWriter((request, response) -> {
                            // 스캐너가 요구하는 XSS 관련 헤더들
                            response.setHeader("X-XSS-Protection", "1; mode=block");
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            response.setHeader("X-Frame-Options", "DENY");
                            // 너무 빡세지 않게 기본 CSP
                            response.setHeader("Content-Security-Policy",
                                    "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(EXCLUDE_URLS).permitAll()
                        .requestMatchers("/admin/billing/export/excel").permitAll()
                        .requestMatchers("/admin/user/bulk-upload/template").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Admin API 전체
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // SuperAdmin API
                        .requestMatchers("/superadmin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // 모든 나머지 요청은 USER / ADMIN / SUPER_ADMIN 허용
                        .anyRequest().hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:8080",
                "https://denti.thomabio.com"
        ));

        // ⚠ PATCH 반드시 추가해야 함!
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Content-Type", "Authorization", "X-Requested-With",
                "Accept", "Origin"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



}
