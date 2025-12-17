package com.kaii.dentix.domain.jwt;

import com.kaii.dentix.global.common.error.ErrorResponse;
import com.kaii.dentix.global.common.error.exception.TokenExpiredException;
import com.kaii.dentix.global.common.response.ResponseMessage;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static com.kaii.dentix.global.config.WebSecurityConfig.EXCLUDE_URLS;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    /** 🔥 JWT 검사 제외 경로 (파일 다운로드, 헬스체크 등) */
    private static final String[] JWT_EXCLUDE_PATHS = {
            "/login",                  // 로그인
            "/password",               // 비밀번호 질문/찾기
            "/verify",                 // 회원 확인
            "/actuator/health",        // 헬스체크
            "/admin/user/bulk-upload/template",
            "/admin/billing/export/excel"
    };

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String uri = request.getRequestURI();
        log.info("[JWT Filter] requestURI={}", uri);

        /* =========================
           1️⃣ JWT 검사 제외 경로
           ========================= */
        for (String path : JWT_EXCLUDE_PATHS) {
            if (uri.startsWith(path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        /* =========================
           2️⃣ Authorization 헤더 추출
           ========================= */
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.isBlank(accessToken)) {
            log.warn("Authorization 헤더 없음");
            ErrorResponse.of(response, HttpStatus.FORBIDDEN, ResponseMessage.FORBIDDEN_MSG);
            return;
        }

        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        try {
            /* =========================
              토큰 검증
               ========================= */
            if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)) {
                throw new TokenExpiredException();
            }

            if (jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                throw new TokenExpiredException();
            }

            /* ===
            SecurityContext 등록
               ========================= */
            Authentication authentication =
                    jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);

            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("SecurityContext 설정 완료: {}", authentication.getAuthorities());
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage());
            ErrorResponse.of(response, HttpStatus.FORBIDDEN, ResponseMessage.FORBIDDEN_MSG);
        }
    }
}