package com.kaii.dentix.domain.jwt;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtSessionSecurityTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AdminRepository admins = mock(AdminRepository.class);
    private final JwtTokenUtil jwt = new JwtTokenUtil(users, admins);
    @BeforeEach void keys() {
        ReflectionTestUtils.setField(jwt, "accessTokenKeyRaw", "test-access-key-for-security-tests-only-32");
        ReflectionTestUtils.setField(jwt, "refreshTokenKeyRaw", "test-refresh-key-for-security-tests-only-32");
        jwt.init();
    }
    @Test void logoutAndSubsequentLoginNeverReviveOldBoundAccessToken() {
        User user = User.builder().userId(7L).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        user.updateLogin(jwt.createToken(user, TokenType.RefreshToken));
        String access = jwt.createToken(user, TokenType.AccessToken);
        assertThat(jwt.isUnauthorized(access, TokenType.AccessToken)).isFalse();
        user.logout();
        assertThat(jwt.isUnauthorized(access, TokenType.AccessToken)).isTrue();
        user.updateLogin(jwt.createToken(user, TokenType.RefreshToken));
        assertThat(jwt.isUnauthorized(access, TokenType.AccessToken)).isTrue();
        assertThat(jwt.isUnauthorized(jwt.createToken(user, TokenType.AccessToken), TokenType.AccessToken)).isFalse();
    }
    @Test void demotedAdministratorCannotUseOldSuperAdminClaim() {
        Admin superAdmin = Admin.builder().adminId(9L).adminIsSuper(YnType.Y).build();
        superAdmin.updateAdminLogin(jwt.createToken(superAdmin, TokenType.RefreshToken));
        String token = jwt.createToken(superAdmin, TokenType.AccessToken);
        Admin demoted = Admin.builder().adminId(9L).adminIsSuper(YnType.N).adminRefreshToken(superAdmin.getAdminRefreshToken()).build();
        when(admins.findById(9L)).thenReturn(Optional.of(demoted));
        assertThat(jwt.getAuthentication(token, TokenType.AccessToken).getAuthorities())
                .extracting(a -> a.getAuthority()).containsExactly("ROLE_ADMIN");
    }
    @Test void refreshTokensFromRapidLoginsAreDistinct() {
        User user = User.builder().userId(7L).build();
        assertThat(jwt.createToken(user, TokenType.RefreshToken)).isNotEqualTo(jwt.createToken(user, TokenType.RefreshToken));
    }

    @Test void freshAccessTokenCannotExtendAnExpiredStoredRefreshSession() {
        User user = User.builder().userId(7L).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        javax.crypto.SecretKey key = (javax.crypto.SecretKey) ReflectionTestUtils.getField(jwt, "refreshTokenKey");
        String expiredRefresh = io.jsonwebtoken.Jwts.builder().subject("7").claim("roles", "ROLE_USER")
                .expiration(new java.util.Date(System.currentTimeMillis() - 1000)).signWith(key).compact();
        user.updateLogin(expiredRefresh);
        assertThat(jwt.isUnauthorized(jwt.createToken(user, TokenType.AccessToken), TokenType.AccessToken)).isTrue();
    }
    @Test void passwordChangeRevokesAccessAndRefreshSession() {
        User user = User.builder().userId(7L).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        String refresh = jwt.createToken(user, TokenType.RefreshToken); user.updateLogin(refresh);
        String access = jwt.createToken(user, TokenType.AccessToken);
        user.modifyUserPassword(mock(org.springframework.security.crypto.password.PasswordEncoder.class), "unused-test-password");
        assertThat(jwt.isUnauthorized(access, TokenType.AccessToken)).isTrue();
        assertThat(jwt.isUnauthorized(refresh, TokenType.RefreshToken)).isTrue();
    }
}
