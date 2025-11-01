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

        final String uri = requestURI;

        boolean permitAll = Arrays.stream(EXCLUDE_URLS)
                .anyMatch(url ->
                        url.endsWith("*")
                                ? uri.startsWith(url.substring(0, url.length() - 1))
                                : uri.equals(url)
                );

        // ✅ 엑셀 양식 다운로드는 JWT 인증 예외
        if (uri.startsWith("/admin/user/bulk-upload/template")) {
            permitAll = true;
        }

        log.info("[JWT Filter] requestURI={}, permitAll={}", requestURI, permitAll);

        if (!permitAll) {
            try {
                String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

                if (StringUtils.isBlank(accessToken)) {
                    throw new TokenExpiredException();
                }

                if (accessToken.startsWith("Bearer ")) {
                    accessToken = accessToken.substring(7);
                }

                if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)) {
                    throw new TokenExpiredException();
                }

                if (jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                    throw new TokenExpiredException();
                }

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

        filterChain.doFilter(request, response);
    }
}