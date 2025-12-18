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

//    private final UserDeviceTypeService userDeviceTypeService;

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

    /**
     *  비밀번호 암호화
     */
    @Bean
    public PasswordEncoder PasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. FilterChain 설정 변경
    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("X-XSS-Protection", "1; mode=block");
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            response.setHeader("X-Frame-Options", "DENY");
                            response.setHeader("Content-Security-Policy",
                                    "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. OPTIONS 메서드는 무조건 가장 먼저 허용 (CORS 해결)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. 엑셀 및 템플릿 경로를 명확하게 최상단 배치
                        .requestMatchers(
                                "/admin/billing/export/**"
                        ).hasAnyRole("ADMIN", "SUPER_ADMIN","ROLE_ADMIN")
                        .requestMatchers("/admin/user/bulk-upload/template/**").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        // 3. 기존 배열 적용
                        .requestMatchers(EXCLUDE_URLS).permitAll()

                        // 4. 나머지 권한 검사
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class);
        log.error("AUTH = {}", SecurityContextHolder.getContext().getAuthentication());
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "https://denti.thomabio.com"
        ));

        // 2. 메서드 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. 헤더 허용 (중복 제거 및 명시)
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // 4. 프론트에서 인증 정보(쿠키/헤더)를 보낼 수 있도록 허용
        configuration.setAllowCredentials(true);

        // 5. Preflight 요청을 브라우저에 캐싱 (매번 OPTIONS 요청을 보내지 않도록)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
