package com.kaii.dentix.global.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/** Per-instance abuse protection; forwarded headers are not interpreted here. */
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private final Clock clock;
    private final Cache<String, Window> windows = Caffeine.newBuilder().maximumSize(20_000)
            .expireAfterAccess(Duration.ofMinutes(2)).build();
    public RequestRateLimitFilter() { this(Clock.systemUTC()); }
    RequestRateLimitFilter(Clock clock) { this.clock = clock; }
    private static final class Window {
        long minute = -1;
        int used;
        synchronized boolean accept(long currentMinute, int limit) {
            if (minute != currentMinute) { minute = currentMinute; used = 0; }
            if (used >= limit) return false;
            used++;
            return true;
        }
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String key = null;
        int limit = 0;
        if ("POST".equals(request.getMethod()) && path.equals("/login")) {
            // Institutions can share an IP. Account-specific lockout needs shared storage.
            key = "login:" + request.getRemoteAddr(); limit = 240;
        } else if ("POST".equals(request.getMethod()) && (path.startsWith("/oralCheck/") || path.equals("/tts/speech"))) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                key = path.startsWith("/oralCheck/") ? "analysis:" : "tts:";
                key += auth.getName(); limit = path.startsWith("/oralCheck/") ? 30 : 120;
            }
        }
        if (key != null && !windows.get(key, ignored -> new Window()).accept(clock.millis() / 60_000, limit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"rt\":429,\"rtMsg\":\"요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
