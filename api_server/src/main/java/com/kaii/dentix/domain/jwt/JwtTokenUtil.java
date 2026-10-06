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
        if (tokenType == TokenType.AccessToken) claims.put("session", sessionHash(admin.getAdminRefreshToken()));
        claims.put("adminIsSuper", admin.getAdminIsSuper().name());

        if (admin.getOrganization() != null) {
            claims.put("organizationId", admin.getOrganization().getOrganizationId());
        }

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
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
        if (tokenType == TokenType.AccessToken) claims.put("session", sessionHash(user.getUserRefreshToken()));

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setId(UUID.randomUUID().toString())
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

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** ------------------------------
     *  Authentication 생성
     * ------------------------------ */
    public UsernamePasswordAuthenticationToken getAuthentication(String token, TokenType tokenType) {
        Claims claims = getClaims(token, tokenType);
        String role = claims.get("roles", String.class);
        String isSuper = "N";
        if (UserRole.ROLE_ADMIN.name().equals(role)) {
            Admin admin = adminRepository.findById(Long.valueOf(claims.getSubject()))
                    .orElseThrow(com.kaii.dentix.global.common.error.exception.UnauthorizedException::new);
            isSuper = admin.isSuperAdmin() ? "Y" : "N";
        } else if (!UserRole.ROLE_USER.name().equals(role)) {
            throw new com.kaii.dentix.global.common.error.exception.UnauthorizedException();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role)); // ROLE_ADMIN

        if ("Y".equalsIgnoreCase(isSuper)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        claims.getSubject(),
                        "",
                        authorities
                );

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public String getAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) return null;

        if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
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
            return userRepository.findById(id).map(user -> !validSession(token, tokenType, user.getUserRefreshToken(), user.getUserLastLoginDate())).orElse(true);
        }
        if (role == UserRole.ROLE_ADMIN) {
            return adminRepository.findById(id).map(admin -> !validSession(token, tokenType, admin.getAdminRefreshToken(), admin.getAdminLastLoginDate())).orElse(true);
        }

        return true;
    }

    private boolean validSession(String token, TokenType tokenType, String storedRefresh, Date lastLogin) {
        if (storedRefresh == null || storedRefresh.isBlank()) return false;
        if (isExpired(storedRefresh, TokenType.RefreshToken)) return false;
        if (tokenType == TokenType.RefreshToken) return java.security.MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), storedRefresh.getBytes(StandardCharsets.UTF_8));
        Claims claims = getClaims(token, tokenType);
        String session = claims.get("session", String.class);
        if (session != null) return java.security.MessageDigest.isEqual(
                session.getBytes(StandardCharsets.UTF_8), sessionHash(storedRefresh).getBytes(StandardCharsets.UTF_8));
        // Preserve already-issued sessions during rollout. Legacy tokens cannot outlive their refresh token.
        return lastLogin == null || claims.getIssuedAt().getTime() / 1000 >= lastLogin.getTime() / 1000;
    }

    private String sessionHash(String refreshToken) {
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((refreshToken == null ? "" : refreshToken).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Session hashing is unavailable.", exception);
        }
    }

    public Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("인증된 관리자가 없습니다.");
        return Long.valueOf(auth.getName());
    }

    public boolean isSuperAdmin(HttpServletRequest request) {
        String token = getAccessToken(request);
        if (token == null) return false;

        if (getRoles(token, TokenType.AccessToken) != UserRole.ROLE_ADMIN) return false;
        return adminRepository.findById(getUserId(token, TokenType.AccessToken)).map(Admin::isSuperAdmin).orElse(false);
    }

    public Long getOrganizationIdFromToken(HttpServletRequest request) {
        String token = getAccessToken(request);
        if (token == null) return null;

        if (getRoles(token, TokenType.AccessToken) != UserRole.ROLE_ADMIN) return null;
        return adminRepository.findByIdWithOrganization(getUserId(token, TokenType.AccessToken))
                .map(Admin::getOrganization).map(com.kaii.dentix.domain.organization.domain.Organization::getOrganizationId)
                .orElse(null);
    }
}
