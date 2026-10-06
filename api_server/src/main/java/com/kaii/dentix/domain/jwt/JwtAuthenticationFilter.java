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

        String accessToken = jwtTokenUtil.getAccessToken(request);
        if (accessToken != null) {
            try {
                if (!jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)
                        && !jwtTokenUtil.isUnauthorized(accessToken, TokenType.AccessToken)) {
                    Authentication authentication = jwtTokenUtil.getAuthentication(accessToken, TokenType.AccessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
                log.warn("JWT authentication rejected. type={}", exception.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }
}
