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
            "/admin/billing/export/excel/*",
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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 배열 대신 명시적으로 먼저 허용 (순서가 중요합니다!)
                        .requestMatchers("/admin/billing/export/excel").permitAll()
                        .requestMatchers("/admin/user/bulk-upload/template").permitAll()

                        // 그 다음 배열 적용
                        .requestMatchers(EXCLUDE_URLS).permitAll()

                        // 나머지 권한 설정
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 만약 ROLE_ 이슈가 의심된다면 아래처럼 변경
                        // .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ADMIN", "SUPER_ADMIN")

                        .anyRequest().hasAnyRole("USER", "ADMIN", "SUPER_ADMIN")
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. 요청을 보내는 프론트엔드 도메인을 정확히 명시
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:8080",
                "https://denti.thomabio.com" // 반드시 포함되어야 함
        ));

        // 2. 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. 허용할 헤더 (Authorization 헤더 필수)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // 4. 자격 증명 허용 (쿠키나 Authorization 헤더 사용 시 true)
        configuration.setAllowCredentials(true);

        // 5. 브라우저가 캐시할 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
