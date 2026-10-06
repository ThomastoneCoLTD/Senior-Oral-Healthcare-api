package com.kaii.dentix.domain.admin;

import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.domain.AdminApprovalStatus;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.findPwdQuestion.domain.FindPwdQuestion;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.security.AdminAccessGuard;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminApprovalServiceTest {
    @Mock AdminAccessGuard guard;
    @Mock ModelMapper mapper;
    @Mock JwtTokenUtil jwt;
    @Mock AdminRepository repository;
    @Mock PasswordEncoder passwords;
    @Mock FindPwdQuestionRepository questions;
    @InjectMocks AdminService service;

    @Test void publicSignupCreatesPendingAccountWithoutTokensOrInstitution() {
        when(questions.findById(1L)).thenReturn(Optional.of(mock(FindPwdQuestion.class)));
        when(passwords.encode("test-password")).thenReturn("test-hash");
        when(repository.save(any())).thenAnswer(call -> { Admin admin=call.getArgument(0); admin.setAdminId(7L); return admin; });
        service.adminSignUp(AdminAuthDto.SignUpRequest.builder().loginId("applicant").name("신청자")
                .phoneNumber("01000000000").password("test-password").findPwdQuestionId(1L).findPwdAnswer("answer").build());
        ArgumentCaptor<Admin> saved = ArgumentCaptor.forClass(Admin.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getApprovalStatus()).isEqualTo(AdminApprovalStatus.PENDING);
        assertThat(saved.getValue().isApproved()).isFalse();
        assertThat(saved.getValue().getAdminIsSuper()).isEqualTo(YnType.N);
        assertThat(saved.getValue().getOrganization()).isNull();
        assertThat(saved.getValue().getAdminRefreshToken()).isNull();
        verifyNoInteractions(jwt, guard);
    }

    @Test void approvalRecordsApproverAndIsIdempotentWithoutPromotingPrivileges() {
        Admin admin = Admin.builder().adminId(7L).adminIsSuper(YnType.N).approvalStatus(AdminApprovalStatus.PENDING).build();
        when(guard.currentAdmin()).thenReturn(Admin.builder().adminId(1L).adminIsSuper(YnType.Y).build());
        when(repository.findByIdForApproval(7L)).thenReturn(Optional.of(admin));
        assertThat(service.approveAdmin(7L).getApprovalStatus()).isEqualTo(AdminApprovalStatus.APPROVED);
        var approvedAt = admin.getApprovedAt();
        assertThat(approvedAt).isNotNull();
        assertThat(admin.getApprovedByAdminId()).isEqualTo(1L);
        assertThat(admin.getAdminIsSuper()).isEqualTo(YnType.N);
        assertThat(admin.getOrganization()).isNull();
        service.approveAdmin(7L);
        assertThat(admin.getApprovedAt()).isEqualTo(approvedAt);
        verify(guard,times(2)).requireSuperAdmin();
        verifyNoInteractions(jwt);
    }

    @Test void unauthorizedApprovalDoesNotLoadOrChangeTarget() {
        doThrow(new UnauthorizedException()).when(guard).requireSuperAdmin();
        assertThatThrownBy(() -> service.approveAdmin(7L)).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(repository);
    }

    @Test void legacyAccountKeepsExistingAccessWithoutInventingApprovalAudit() {
        Admin admin = Admin.builder().adminId(7L).adminIsSuper(YnType.N).build();
        assertThat(admin.isApproved()).isTrue();
        admin.approve(1L);
        assertThat(admin.getApprovalStatus()).isNull();
        assertThat(admin.getApprovedAt()).isNull();
        assertThat(admin.getApprovedByAdminId()).isNull();
    }
}
