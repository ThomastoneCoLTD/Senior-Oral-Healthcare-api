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

            // 토큰이 없으면 에러를 던지지 말고, 그냥 다음 필터로 넘깁니다.
            // 그러면 WebSecurityConfig에 설정한 permitAll()이 정상 작동합니다.
            if (StringUtils.isNotBlank(accessToken)) {
                if (accessToken.startsWith("Bearer ")) {
                    accessToken = accessToken.substring(7);
                }

                if (!jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken) &&
                        !jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {

                    Authentication authentication = jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);
                    if (authentication != null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JWT 인증 처리 중 예외 발생 (무시하고 진행): {}", e.getMessage());
            // 여기서 return 하지 않고 계속 진행하게 하면, 권한이 필요한 페이지는
            // 나중에 AuthorizationFilter에서 알아서 거부합니다.
        }

        //7) 다음 필터로
        filterChain.doFilter(request, response);
    }
}
