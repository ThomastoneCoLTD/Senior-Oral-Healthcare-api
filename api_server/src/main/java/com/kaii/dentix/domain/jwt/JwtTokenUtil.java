package com.kaii.dentix.domain.jwt;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
//@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    @Value("${jwt.accessTokenKey}")
    private String accessTokenKey;

    @Value("${jwt.refreshTokenKey}")
    private String refreshTokenKey;

    private final UserRepository userRepository;

    private final AdminRepository adminRepository;

    // 객체 초기화, secretKey를 Base64로 인코딩한다.
    @PostConstruct
    protected void init() {
        accessTokenKey = Base64.getEncoder().encodeToString(accessTokenKey.getBytes());
        refreshTokenKey = Base64.getEncoder().encodeToString(refreshTokenKey.getBytes());
    }

    public String createToken(User user, TokenType tokenType) {
        return this.createToken(user.getUserId(), UserRole.ROLE_USER, tokenType);
    }
//수정 전_오류나면 이거 살리기
//    public String createToken(Admin admin, TokenType tokenType) {
//        return this.createToken(admin.getAdminId(), UserRole.ROLE_ADMIN, tokenType);
//    }

    public String createToken(Admin admin, TokenType tokenType) {
        String secretKey = tokenType.equals(TokenType.AccessToken) ? accessTokenKey : refreshTokenKey;

        Claims claims = Jwts.claims().setSubject(String.valueOf(admin.getAdminId()));
        claims.put("roles", UserRole.ROLE_ADMIN);
        claims.put("adminIsSuper", admin.getAdminIsSuper().name());
        // ✅ organizationId 추가 (없을 수도 있으므로 null-safe)
        if (admin.getOrganization() != null) {
            claims.put("organizationId", admin.getOrganization().getOrganizationId());
        }

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenType.getValidTime()))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // JWT 토큰 인증정보 생성
    private String createToken(Long userId, UserRole role, TokenType tokenType) {
        String secretKey = tokenType.equals(TokenType.AccessToken) ? accessTokenKey : refreshTokenKey;

        Claims claims = Jwts.claims().setSubject(String.valueOf(userId)); // JWT payload에 저장되는 정보 단위
        claims.put("roles", role); // 정보는 key / value 쌍으로 저장된다.
        Date now = new Date();
        return Jwts.builder()
            .setClaims(claims) // 정보 저장
            //.setIssuer("https://ondentii-dev.kai-i.com")
            .setIssuedAt(now) // 토큰 발행시간 정보
            .setExpiration(new Date(now.getTime() + tokenType.getValidTime())) // set Expire Time (유효기간)
            .signWith(SignatureAlgorithm.HS256, secretKey) // 사용할 암호화 알고리즘과 signature 에 들어갈 secret 값 셋팅
            .compact();
    }
    public Long getOrganizationIdFromAccessToken(HttpServletRequest request) {
        String token = getAccessToken(request);
        if (token == null) return null;
        try {
            return getClaims(token, TokenType.AccessToken).get("organizationId", Long.class);
        } catch (Exception e) {
            return null;
        }
    }
    public Claims getClaims(String token, TokenType tokenType) {
        String secretKey = tokenType.equals(TokenType.AccessToken) ? accessTokenKey : refreshTokenKey;

        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    // JWT 토큰 인증정보 조회
    public UsernamePasswordAuthenticationToken getAuthentication(String token, TokenType tokenType) {
        Claims claims = this.getClaims(token, tokenType);
        String role = claims.get("roles", String.class);
        Long userId = Long.parseLong(claims.getSubject());

        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        // ✅ principal을 Claims 대신 UserDetails 로 세팅
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        String.valueOf(userId), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // Request의 Header에서 access token 값을 가져옵니다. "Authorization" : "access token 값"
    public String getAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }

        // 대소문자 구분 없이, 공백 유무도 유연하게 처리
        if (header.toLowerCase().startsWith("bearer")) {
            // "bearer" 이후 공백이 있으면 제거하고 나머지 반환
            return header.substring(6).trim();
        }

        // 혹시 그냥 토큰만 들어온 경우도 대비
        return header.trim();
    }

    // Request의 Header에서 refresh token 값을 가져옵니다. "refreshToken" : "refresh token 값"
    public String getRefreshToken(HttpServletRequest request) {
        return request.getHeader("RefreshToken");
    }

    // 토큰 유효성 + 만료일자 확인
    public boolean isExpired(String token, TokenType tokenType) {
        try {
            return this.getClaims(token, tokenType).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // 토큰에서 회원정보 추출
    public Long getUserId(String token, TokenType tokenType) {
        String result = this.getClaims(token, tokenType).getSubject();
        return Long.parseLong(result);
    }

    public UserRole getRoles(String token, TokenType tokenType) {
        return UserRole.valueOf(String.valueOf(this.getClaims(token, tokenType).get("roles")));
    }

    /**
     * UserRole 확인
     */
    public boolean isUnauthorized(String token, TokenType tokenType) {

        Long userId = this.getUserId(token, tokenType);
        UserRole roles = this.getRoles(token, tokenType);

        switch (roles) {
            case ROLE_USER:
                User user = userRepository.findById(userId).orElse(null);
                return user == null;
            case ROLE_ADMIN:
                Admin admin = adminRepository.findById(userId).orElse(null);
                return admin == null;
            default:
                return true;
        }
    }

    public Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증된 관리자가 없습니다.");
        }
        String adminId = authentication.getName(); // JWT의 subject가 adminId
        return Long.parseLong(adminId);
    }
    public boolean isSuperAdmin(HttpServletRequest request) {
        String token = getAccessToken(request);
        if (token == null) return false;
        try {
            Claims claims = getClaims(token, TokenType.AccessToken);
            String isSuper = claims.get("adminIsSuper", String.class);
            return "Y".equalsIgnoreCase(isSuper);
        } catch (Exception e) {
            return false;
        }
    }



}
