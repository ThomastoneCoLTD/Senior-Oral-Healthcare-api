package com.kaii.dentix.domain.user;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.findPwdQuestion.domain.FindPwdQuestion;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.application.DaeguDefaultOrganizationService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.passwordReset.application.PasswordResetService;
import com.kaii.dentix.domain.passwordReset.domain.PasswordResetAccountType;
import com.kaii.dentix.domain.passwordReset.dto.PasswordResetDto;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserDaeguProvisioningService;
import com.kaii.dentix.domain.user.dao.UserLoginHistoryRepository;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserLoginServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private FindPwdQuestionRepository findPwdQuestionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminRepository adminRepository;
    @Mock private DaeguDefaultOrganizationService daeguDefaultOrganizationService;
    @Mock private ServiceAgreementConsentService serviceAgreementConsentService;
    @Mock private UserDaeguProvisioningService userDaeguProvisioningService;
    @Mock private DaeguChainDidService daeguChainDidService;
    @Mock private UserLoginHistoryRepository userLoginHistoryRepository;
    @Mock private PasswordResetService passwordResetService;

    @InjectMocks
    private UserLoginService userLoginService;

    @Test
    void phoneCheckReturnsExistingUserOnlyWhenNameAndPhoneMatch() {
        User user = User.builder()
                .userId(1L)
                .userName("김덴티")
                .userPhoneNumber("01012345678")
                .build();
        given(userRepository.findByUserPhoneNumber("01012345678")).willReturn(Optional.of(user));

        UserDto.VerifyResponse response = userLoginService.userPhoneCheck(
                UserDto.VerifyRequest.builder()
                        .userName(" 김덴티 ")
                        .userPhoneNumber("010-1234-5678")
                        .build()
        );

        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void phoneCheckRejectsDifferentNameForRegisteredPhone() {
        User user = User.builder()
                .userId(1L)
                .userName("김덴티")
                .userPhoneNumber("01012345678")
                .build();
        given(userRepository.findByUserPhoneNumber("01012345678")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userLoginService.userPhoneCheck(
                UserDto.VerifyRequest.builder()
                        .userName("이덴티")
                        .userPhoneNumber("01012345678")
                        .build()
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("이름과 전화번호가 일치하지 않습니다.");
    }

    @Test
    void userSignUpStoresSelectedRealOrganization() {
        Organization organization = Organization.builder()
                .organizationId(10L)
                .organizationName("Token Admin Organization")
                .build();
        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.empty());
        given(userRepository.findByUserPhoneNumber("01012345678")).willReturn(Optional.empty());
        given(findPwdQuestionRepository.findById(1L)).willReturn(Optional.of(FindPwdQuestion.builder()
                .findPwdQuestionId(1L)
                .findPwdQuestionSort(1L)
                .findPwdQuestionTitle("질문")
                .build()));
        given(daeguDefaultOrganizationService.getTokenAdminOrganization()).willReturn(organization);
        given(passwordEncoder.encode("password!")).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(userDaeguProvisioningService.provisionForSignUp(any(User.class))).willReturn("0x123");
        given(jwtTokenUtil.createToken(any(User.class), any(TokenType.class))).willReturn("token");

        UserDto.SignUpResponse response = userLoginService.userSignUp(UserDto.SignUpRequest.builder()
                .userLoginIdentifier("dentix123")
                .userPassword("password!")
                .userName("김덴티")
                .userGender(GenderType.W)
                .userPhoneNumber("01012345678")
                .userBirthDate("1950-01-01")
                .realOrganization("대구2")
                .findPwdQuestionId(1L)
                .findPwdAnswer("답변")
                .userServiceAgreementRequest(List.of(1L, 2L))
                .build());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRealOrganization()).isEqualTo("대구2");
        assertThat(userCaptor.getValue().getUserBirthDate()).isEqualTo("1950-01-01");
        assertThat(response.getRealOrganization()).isEqualTo("대구2");
    }

    @Test
    void findUserLoginIdentifierReturnsIdWhenIdentityFieldsMatch() {
        User user = User.builder()
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPhoneNumber("01012345678")
                .userBirthDate("1950-01-01")
                .build();
        given(userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                "01012345678", "김덴티", "1950-01-01"))
                .willReturn(Optional.of(user));

        UserDto.FindIdResponse response = userLoginService.findUserLoginIdentifier(
                UserDto.FindIdRequest.builder()
                        .userName(" 김덴티 ")
                        .userPhoneNumber("010-1234-5678")
                        .userBirthDate("1950-01-01")
                        .build()
        );

        assertThat(response.getUserLoginIdentifier()).isEqualTo("dentix123");
    }

    @Test
    void findUserLoginIdentifierRejectsMismatchedBirthDate() {
        given(userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                "01012345678", "김덴티", "1951-01-01"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userLoginService.findUserLoginIdentifier(
                UserDto.FindIdRequest.builder()
                        .userName("김덴티")
                        .userPhoneNumber("01012345678")
                        .userBirthDate("1951-01-01")
                        .build()
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("입력한 정보가 일치하지 않습니다.");
    }

    @Test
    void findPasswordIssuesOneTimeTokenWhenQuestionAndAnswerMatch() {
        User user = User.builder()
                .userId(1L)
                .userLoginIdentifier("dentix123")
                .findPwdQuestionId(2L)
                .findPwdAnswer("대구초등학교")
                .build();
        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.of(user));
        given(passwordResetService.issue(PasswordResetAccountType.USER, 1L, "dentix123"))
                .willReturn(PasswordResetDto.IssueResponse.builder()
                        .resetToken("one-time-reset-token")
                        .expiresInSeconds(600)
                        .loginIdentifier("dentix123")
                        .build());

        UserDto.FindPasswordResponse response = userLoginService.userFindPassword(
                UserDto.FindPasswordRequest.builder()
                        .userLoginIdentifier(" dentix123 ")
                        .findPwdQuestionId(2L)
                        .findPwdAnswer(" 대구초등학교 ")
                        .build()
        );

        assertThat(response.getResetToken()).isEqualTo("one-time-reset-token");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600);
    }

    @Test
    void resetPasswordConsumesTokenAndInvalidatesLoginSession() {
        User user = User.builder()
                .userId(1L)
                .userPassword("old-password")
                .userRefreshToken("old-refresh-token")
                .build();
        given(passwordResetService.consume(PasswordResetAccountType.USER, "one-time-reset-token"))
                .willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPassword!")) .willReturn("encoded-new-password");

        userLoginService.userModifyPassword(UserDto.ModifyPasswordRequest.builder()
                .resetToken("one-time-reset-token")
                .userPassword("newPassword!")
                .build());

        assertThat(user.getUserPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getUserRefreshToken()).isNull();
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void userDidSignUpStoresSelectedGenderWithoutExposingPasswordRecovery() {
        Organization organization = Organization.builder()
                .organizationId(10L)
                .organizationName("Token Admin Organization")
                .build();
        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.empty());
        given(userRepository.findByUserPhoneNumber("01012345678")).willReturn(Optional.empty());
        given(findPwdQuestionRepository.findById(1L)).willReturn(Optional.of(FindPwdQuestion.builder()
                .findPwdQuestionId(1L)
                .findPwdQuestionSort(1L)
                .findPwdQuestionTitle("기억에 남는 장소는?")
                .build()));
        given(daeguDefaultOrganizationService.getTokenAdminOrganization()).willReturn(organization);
        given(passwordEncoder.encode(any(String.class))).willReturn("encoded-placeholder");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(userDaeguProvisioningService.provisionForSignUp(any(User.class))).willReturn("0x123");
        given(jwtTokenUtil.createToken(any(User.class), any(TokenType.class))).willReturn("token");

        userLoginService.userDidSignUp(UserDto.DidSignUpRequest.builder()
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userGender(GenderType.W)
                .userPhoneNumber("010-1234-5678")
                .userBirthDate("1950-01-01")
                .realOrganization("대구1")
                .userServiceAgreementRequest(List.of(1L, 2L))
                .build());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUserGender()).isEqualTo(GenderType.W);
        assertThat(userCaptor.getValue().getFindPwdQuestionId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getFindPwdAnswer()).isNotBlank();
        assertThat(userCaptor.getValue().getFindPwdAnswer()).doesNotContain("dentix123");
    }

    @Test
    void userLoginAuthenticatesWithPasswordWithoutRequiringDid() {
        User user = User.builder()
                .userId(1L)
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPassword("encoded-password")
                .userPhoneNumber("01012345678")
                .findPwdQuestionId(1L)
                .findPwdAnswer("answer")
                .isVerify(YnType.Y)
                .build();

        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password!", "encoded-password")).willReturn(true);
        given(jwtTokenUtil.createToken(user, TokenType.AccessToken)).willReturn("access-token");
        given(jwtTokenUtil.createToken(user, TokenType.RefreshToken)).willReturn("refresh-token");

        UserDto.LoginResponse response = userLoginService.userLogin(UserDto.LoginRequest.builder()
                .userLoginIdentifier("dentix123")
                .userPassword("password!")
                .build());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getUserRefreshToken()).isEqualTo("refresh-token");
        verify(passwordEncoder).matches("password!", "encoded-password");
    }

    @Test
    void userLoginRejectsWrongPassword() {
        User user = User.builder()
                .userId(1L)
                .userLoginIdentifier("dentix123")
                .userName("user")
                .userPassword("encoded-password")
                .userPhoneNumber("01012345678")
                .findPwdQuestionId(1L)
                .findPwdAnswer("answer")
                .isVerify(YnType.Y)
                .build();

        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> userLoginService.userLogin(UserDto.LoginRequest.builder()
                .userLoginIdentifier("dentix123")
                .userPassword("wrong-password")
                .build()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid login identifier or password.");

        verify(passwordEncoder).matches("wrong-password", "encoded-password");
    }
}
