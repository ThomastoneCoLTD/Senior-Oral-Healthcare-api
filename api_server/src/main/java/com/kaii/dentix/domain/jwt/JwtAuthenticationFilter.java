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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

            // 토큰이 없으면 SecurityConfig에서 처리하도록 넘김
            if (StringUtils.isBlank(accessToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!accessToken.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            accessToken = accessToken.substring(7).trim();
            if (accessToken.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
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
