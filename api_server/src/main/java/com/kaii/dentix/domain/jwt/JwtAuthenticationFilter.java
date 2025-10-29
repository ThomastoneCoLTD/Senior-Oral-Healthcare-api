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

        String projectName = "/dentix";
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith(projectName)) {
            requestURI = requestURI.substring(projectName.length());
        }

        final String uri = requestURI; // ✅ 람다에서 사용할 불변 변수

        boolean permitAll = Arrays.stream(EXCLUDE_URLS)
                .anyMatch(url ->
                        url.endsWith("*")
                                ? uri.startsWith(url.substring(0, url.length() - 1))
                                : uri.equals(url)
                );

        log.info("[JWT Filter] requestURI={}, permitAll={}", requestURI, permitAll);

        if (!permitAll) {
            try {
                String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

                if (StringUtils.isBlank(accessToken)) {
                    throw new TokenExpiredException();
                }

                // ✅ Bearer 접두사 제거
                if (accessToken.startsWith("Bearer ")) {
                    accessToken = accessToken.substring(7);
                }

                // ✅ 토큰 검증
                if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)) {
                    throw new TokenExpiredException();
                }

                if (jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                    throw new TokenExpiredException();
                }

                // ✅ SecurityContext 설정
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

        // ✅ JWT 검증 통과 또는 예외 URL이면 다음 필터로
        filterChain.doFilter(request, response);
    }
}
