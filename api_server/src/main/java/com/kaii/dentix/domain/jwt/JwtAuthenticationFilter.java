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

        // ✅ 실제 요청 경로 추출
        String projectName = "/dentix"; // nginx나 프록시 하위 경로 대응
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith(projectName)) {
            requestURI = requestURI.substring(projectName.length());
        }

        final String uri = requestURI;

        // ✅ JWT 인증이 필요 없는 URL 매칭
        boolean permitAll = Arrays.stream(EXCLUDE_URLS)
                .anyMatch(url -> {
                    String normalizedUrl = url.replace("*", "");
                    return uri.startsWith(normalizedUrl);
                });

        // ✅ 추가 예외 케이스 (엑셀 템플릿 다운로드 등)
        if (uri.startsWith("/admin/user/bulk-upload/template")) {
            permitAll = true;
        }

        log.info("[JWT Filter] requestURI={}, permitAll={}", requestURI, permitAll);

        if (!permitAll) {
            try {
                // ✅ Authorization 헤더에서 토큰 추출
                String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

                if (StringUtils.isBlank(accessToken)) {
                    throw new TokenExpiredException();
                }

                if (accessToken.startsWith("Bearer ")) {
                    accessToken = accessToken.substring(7);
                }

                // ✅ 만료 / 비인가 토큰 검증
                if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)) {
                    throw new TokenExpiredException();
                }

                if (jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                    throw new TokenExpiredException();
                }

                // ✅ SecurityContext 등록
                Authentication authentication = jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("✅ SecurityContext 설정 완료: {}", authentication.getAuthorities());
                }

            } catch (Exception e) {
                log.warn("❌ JWT 인증 실패: {}", e.getMessage());
                ErrorResponse.of(response, HttpStatus.FORBIDDEN, ResponseMessage.FORBIDDEN_MSG);
                return;
            }
        }

        // ✅ 예외 경로면 JWT 검사 건너뛰고 다음 필터로
        filterChain.doFilter(request, response);
    }
}
