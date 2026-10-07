package com.kaii.dentix.domain.jwt;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import jakarta.persistence.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Actual JWTs must fit the schema capacity declared by their entity mapping. */
class RefreshTokenStorageTest {
    private final JwtTokenUtil jwt = new JwtTokenUtil(mock(UserRepository.class), mock(AdminRepository.class));

    @BeforeEach
    void keys() {
        ReflectionTestUtils.setField(jwt, "accessTokenKeyRaw", "test-access-key-for-storage-only-32-bytes");
        ReflectionTestUtils.setField(jwt, "refreshTokenKeyRaw", "test-refresh-key-for-storage-only-32-bytes");
        jwt.init();
    }

    @Test
    void superAdministratorCanPersistFullRefreshToken() throws Exception {
        Admin admin = Admin.builder().adminId(Long.MAX_VALUE).adminIsSuper(YnType.Y)
                .organization(Organization.builder().organizationId(Long.MAX_VALUE).build()).build();
        admin.updateAdminLogin(jwt.createToken(admin, TokenType.RefreshToken));
        roundTrip(Admin.class, "adminRefreshToken", admin.getAdminRefreshToken());
    }

    @Test
    void institutionAdministratorCanPersistFullRefreshToken() throws Exception {
        Admin admin = Admin.builder().adminId(Long.MAX_VALUE).adminIsSuper(YnType.N)
                .organization(Organization.builder().organizationId(Long.MAX_VALUE).build()).build();
        admin.updateAdminLogin(jwt.createToken(admin, TokenType.RefreshToken));
        roundTrip(Admin.class, "adminRefreshToken", admin.getAdminRefreshToken());
    }

    @Test
    void userCanPersistFullRefreshToken() throws Exception {
        User user = User.builder().userId(Long.MAX_VALUE).build();
        user.updateLogin(jwt.createToken(user, TokenType.RefreshToken));
        roundTrip(User.class, "userRefreshToken", user.getUserRefreshToken());
    }

    private void roundTrip(Class<?> entity, String field, String token) throws Exception {
        if (entity == Admin.class) assertThat(token.length()).isGreaterThan(255);
        Column mapping = entity.getDeclaredField(field).getAnnotation(Column.class);
        int capacity = mapping == null ? 255 : mapping.length();
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:tokenstorage" + field, "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE token_storage (token VARCHAR(" + capacity + "))");
            }
            try (var insert = connection.prepareStatement("INSERT INTO token_storage(token) VALUES (?)")) {
                insert.setString(1, token);
                insert.executeUpdate();
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT token FROM token_storage")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo(token);
                assertThat(jwt.getClaims(rows.getString(1), TokenType.RefreshToken).getSubject()).isEqualTo(String.valueOf(Long.MAX_VALUE));
            }
        }
    }
}
