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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        //실제 요청 경로 추출
        String projectName = "/dentix"; // nginx나 프록시 하위 경로 대응
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith(projectName)) {
            requestURI = requestURI.substring(projectName.length());
        }

        final String uri = requestURI;

        //2) EXCLUDE_URLS 와 완전 일치 매칭 (prefix 기반)
        boolean permitAll = Arrays.stream(EXCLUDE_URLS)
                .anyMatch(url -> uri.startsWith(url.replace("*", "")));

        // 추가 허용 케이스
        if (uri.startsWith("/admin/user/bulk-upload/template")) {
            permitAll = true;
        }

        log.info("[JWT Filter] requestURI={}, permitAll={}", requestURI, permitAll);

        //3) 인증 필요 없는 경우 → JWT 검사 없이 다음 필터로
        if (permitAll) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

            // 1. 토큰이 아예 없는 경우: 그냥 통과시킨다 (SecurityConfig의 permitAll이 결정하도록)
            if (StringUtils.isBlank(accessToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }

            // 2. 토큰이 있지만 유효하지 않은 경우: 로그만 남기고 통과시킨다
            if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken) ||
                    jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                log.warn("유효하지 않은 토큰 접근");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 유효한 토큰인 경우: 인증 객체 등록
            Authentication authentication = jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("JWT 필터 오류: {}", e.getMessage());
            // 에러 발생 시에도 응답을 직접 끝내지(return) 말고 다음 필터로 넘깁니다.
        }

        filterChain.doFilter(request, response);
    }
}
