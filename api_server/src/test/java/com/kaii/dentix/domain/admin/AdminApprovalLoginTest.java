package com.kaii.dentix.domain.admin;

import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.*;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.jwt.*;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminApprovalLoginTest {
    private final AdminRepository repository=mock(AdminRepository.class);
    private final JwtTokenUtil jwt=mock(JwtTokenUtil.class);
    private final PasswordEncoder passwords=mock(PasswordEncoder.class);
    private final AdminLoginService service=new AdminLoginService(jwt,repository,passwords,null,null);

    @Test void pendingAdminGetsNoTokensUntilSuperAdminApproval() {
        Admin admin=Admin.builder().adminId(7L).adminIsSuper(YnType.N).adminPassword("hash")
                .approvalStatus(AdminApprovalStatus.PENDING).build();
        when(repository.findByAdminLoginIdentifier("applicant")).thenReturn(Optional.of(admin));
        when(passwords.matches("test-password","hash")).thenReturn(true);
        var request=AdminAuthDto.LoginRequest.builder().loginId("applicant").password("test-password").build();
        assertThatThrownBy(() -> service.login(request)).isInstanceOf(UnauthorizedException.class).hasMessageContaining("승인 대기");
        assertThat(admin.getAdminLastLoginDate()).isNull();
        verifyNoInteractions(jwt);
        admin.approve(1L);
        when(jwt.createToken(admin,TokenType.RefreshToken)).thenReturn("refresh");
        when(jwt.createToken(admin,TokenType.AccessToken)).thenReturn("access");
        assertThat(service.login(request).getAccessToken()).isEqualTo("access");
    }

    @Test void legacyAdminCanStillLogIn() {
        Admin admin=Admin.builder().adminId(7L).adminIsSuper(YnType.N).adminPassword("hash").build();
        when(repository.findByAdminLoginIdentifier("existing")).thenReturn(Optional.of(admin));
        when(passwords.matches("test-password","hash")).thenReturn(true);
        when(jwt.createToken(admin,TokenType.RefreshToken)).thenReturn("refresh");
        when(jwt.createToken(admin,TokenType.AccessToken)).thenReturn("access");
        assertThat(service.login(AdminAuthDto.LoginRequest.builder().loginId("existing").password("test-password").build()).getAccessToken()).isEqualTo("access");
    }
}
