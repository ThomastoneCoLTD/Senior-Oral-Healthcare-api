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
            //4) Authorization 헤더에서 토큰 추출
            String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (StringUtils.isBlank(accessToken)) {
                throw new TokenExpiredException();
            }

            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }

            //5) 만료/비인가 토큰 검사
            if (jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)) {
                throw new TokenExpiredException();
            }

            if (jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                throw new TokenExpiredException();
            }

            //6) 인증객체 생성 → SecurityContext 등록
            Authentication authentication =
                    jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);

            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("SecurityContext 설정 완료: {}", authentication.getAuthorities());
            }

        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage());
            ErrorResponse.of(response, HttpStatus.FORBIDDEN, ResponseMessage.FORBIDDEN_MSG);
            return;
        }

        //7) 다음 필터로
        filterChain.doFilter(request, response);
    }
}
