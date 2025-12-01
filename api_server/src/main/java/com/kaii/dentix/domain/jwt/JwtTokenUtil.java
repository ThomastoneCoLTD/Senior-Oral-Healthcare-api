package com.kaii.dentix.domain.jwt;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    @Value("${jwt.accessTokenKey}")
    private String accessTokenKeyRaw;

    @Value("${jwt.refreshTokenKey}")
    private String refreshTokenKeyRaw;

    private SecretKey accessTokenKey;
    private SecretKey refreshTokenKey;

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @PostConstruct
    protected void init() {
        // SecretKey는 반드시 32바이트 이상이어야 함 (HS256)
        this.accessTokenKey = Keys.hmacShaKeyFor(accessTokenKeyRaw.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshTokenKeyRaw.getBytes(StandardCharsets.UTF_8));
    }

    /** ------------------------------
     *  토큰 생성 (Admin)
     * ------------------------------ */
    public String createToken(Admin admin, TokenType tokenType) {
        SecretKey key = tokenType == TokenType.AccessToken ? accessTokenKey : refreshTokenKey;

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", UserRole.ROLE_ADMIN.name());
        claims.put("adminIsSuper", admin.getAdminIsSuper().name());

        if (admin.getOrganization() != null) {
            claims.put("organizationId", admin.getOrganization().getOrganizationId());
        }

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(admin.getAdminId()))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenType.getValidTime()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** ------------------------------
     *  토큰 생성 (User)
     * ------------------------------ */
    public String createToken(User user, TokenType tokenType) {
        SecretKey key = tokenType == TokenType.AccessToken ? accessTokenKey : refreshTokenKey;

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", UserRole.ROLE_USER.name());

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(user.getUserId()))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenType.getValidTime()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** ------------------------------
     *  Claims 조회
     * ------------------------------ */
    public Claims getClaims(String token, TokenType tokenType) {
        SecretKey key = tokenType == TokenType.AccessToken ? accessTokenKey : refreshTokenKey;

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** ------------------------------
     *  Authentication 생성
     * ------------------------------ */
    public UsernamePasswordAuthenticationToken getAuthentication(String token, TokenType tokenType) {
        Claims claims = getClaims(token, tokenType);
        String role = claims.get("roles", String.class);
        Long id = Long.valueOf(claims.getSubject());

        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        String.valueOf(id),
                        "",
                        authorities
                );

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public String getAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) return null;

        if (header.toLowerCase().startsWith("bearer")) {
            return header.substring(6).trim();
        }
        return header.trim();
    }

    public String getRefreshToken(HttpServletRequest request) {
        return request.getHeader("RefreshToken");
    }

    public boolean isExpired(String token, TokenType tokenType) {
        try {
            return getClaims(token, tokenType).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Long getUserId(String token, TokenType tokenType) {
        return Long.valueOf(getClaims(token, tokenType).getSubject());
    }

    public UserRole getRoles(String token, TokenType tokenType) {
        return UserRole.valueOf(getClaims(token, tokenType).get("roles").toString());
    }

    public boolean isUnauthorized(String token, TokenType tokenType) {
        Long id = getUserId(token, tokenType);
        UserRole role = getRoles(token, tokenType);

        if (role == UserRole.ROLE_USER) {
            return userRepository.findById(id).isEmpty();
        }
        if (role == UserRole.ROLE_ADMIN) {
            return adminRepository.findById(id).isEmpty();
        }

        return true;
    }

    public Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("인증된 관리자가 없습니다.");
        return Long.valueOf(auth.getName());
    }

    public boolean isSuperAdmin(HttpServletRequest request) {
        String token = getAccessToken(request);
        if (token == null) return false;

        Claims claims = getClaims(token, TokenType.AccessToken);
        return "Y".equalsIgnoreCase(claims.get("adminIsSuper", String.class));
    }

    public Long getOrganizationIdFromToken(HttpServletRequest request) {
        String token = getAccessToken(request);
        if (token == null) return null;

        Object orgId = getClaims(token, TokenType.AccessToken).get("organizationId");
        if (orgId == null) return null;

        if (orgId instanceof Integer) return ((Integer) orgId).longValue();
        if (orgId instanceof Long) return (Long) orgId;

        return Long.valueOf(orgId.toString());
    }
}
