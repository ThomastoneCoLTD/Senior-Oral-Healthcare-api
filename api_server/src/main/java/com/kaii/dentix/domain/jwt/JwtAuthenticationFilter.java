package com.kaii.dentix.domain.jwt;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static com.kaii.dentix.global.config.WebSecurityConfig.EXCLUDE_URLS;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // [최적화] 로그인 등 인증이 필요 없는 경로는 필터 로직 자체를 실행하지 않음
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        // Nginx 등 프록시 경로 제거 (필요한 경우 유지)
        if (requestURI.startsWith("/dentix")) {
            requestURI = requestURI.substring("/dentix".length());
        }

        String finalUri = requestURI;
        return Arrays.stream(EXCLUDE_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, finalUri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // shouldNotFilter에서 걸러지지 않은 요청만 여기로 옴 (즉, 인증이 필요한 요청들)

        try {
            String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

            // 토큰이 없으면 SecurityConfig에서 처리하도록 넘김
            if (StringUtils.isBlank(accessToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }

            // 토큰 검증
            if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken) ||
                    jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                log.warn("[JWT Filter] 유효하지 않은 토큰입니다.");
                // 여기서 예외를 던지거나, 그냥 넘겨서 401/403 처리를 유도할 수 있음
                // 여기서는 흐름을 끊지 않고 다음 필터로 넘깁니다.
                filterChain.doFilter(request, response);
                return;
            }

            // 인증 객체 생성 및 저장
            Authentication authentication = jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("[JWT Filter] 인증 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}