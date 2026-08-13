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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @InjectMocks
    private UserLoginService userLoginService;

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
                .realOrganization("대구2")
                .findPwdQuestionId(1L)
                .findPwdAnswer("답변")
                .userServiceAgreementRequest(List.of(1L, 2L))
                .build());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRealOrganization()).isEqualTo("대구2");
        assertThat(response.getRealOrganization()).isEqualTo("대구2");
    }

    @Test
    void findUserLoginIdentifierReturnsIdWhenAllRecoveryAnswersMatch() {
        User user = User.builder()
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPhoneNumber("01012345678")
                .findPwdQuestionId(2L)
                .findPwdAnswer("대구초등학교")
                .build();
        given(userRepository.findByUserPhoneNumberAndUserName("01012345678", "김덴티"))
                .willReturn(Optional.of(user));

        UserDto.FindIdResponse response = userLoginService.findUserLoginIdentifier(
                UserDto.FindIdRequest.builder()
                        .userName(" 김덴티 ")
                        .userPhoneNumber("010-1234-5678")
                        .findPwdQuestionId(2L)
                        .findPwdAnswer(" 대구초등학교 ")
                        .build()
        );

        assertThat(response.getUserLoginIdentifier()).isEqualTo("dentix123");
    }

    @Test
    void findUserLoginIdentifierRejectsMismatchedAnswer() {
        User user = User.builder()
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPhoneNumber("01012345678")
                .findPwdQuestionId(2L)
                .findPwdAnswer("대구초등학교")
                .build();
        given(userRepository.findByUserPhoneNumberAndUserName("01012345678", "김덴티"))
                .willReturn(Optional.of(user));

        assertThatThrownBy(() -> userLoginService.findUserLoginIdentifier(
                UserDto.FindIdRequest.builder()
                        .userName("김덴티")
                        .userPhoneNumber("01012345678")
                        .findPwdQuestionId(2L)
                        .findPwdAnswer("다른 답변")
                        .build()
        ))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("입력한 정보가 일치하지 않습니다.");
    }

    @Test
    void userDidSignUpStoresSelectedIdRecoveryQuestionAndAnswer() {
        Organization organization = Organization.builder()
                .organizationId(10L)
                .organizationName("Token Admin Organization")
                .build();
        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.empty());
        given(userRepository.findByUserPhoneNumber("01012345678")).willReturn(Optional.empty());
        given(findPwdQuestionRepository.findById(2L)).willReturn(Optional.of(FindPwdQuestion.builder()
                .findPwdQuestionId(2L)
                .findPwdQuestionSort(2L)
                .findPwdQuestionTitle("졸업한 초등학교는?")
                .build()));
        given(daeguDefaultOrganizationService.getTokenAdminOrganization()).willReturn(organization);
        given(passwordEncoder.encode("DID_ONLY:dentix123")).willReturn("encoded-placeholder");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(userDaeguProvisioningService.provisionForSignUp(any(User.class))).willReturn("0x123");
        given(jwtTokenUtil.createToken(any(User.class), any(TokenType.class))).willReturn("token");

        userLoginService.userDidSignUp(UserDto.DidSignUpRequest.builder()
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPhoneNumber("010-1234-5678")
                .userBirthDate("1950-01-01")
                .realOrganization("대구1")
                .findPwdQuestionId(2L)
                .findPwdAnswer(" 대구초등학교 ")
                .userServiceAgreementRequest(List.of(1L, 2L))
                .build());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFindPwdQuestionId()).isEqualTo(2L);
        assertThat(userCaptor.getValue().getFindPwdAnswer()).isEqualTo("대구초등학교");
    }

    @Test
    void userLoginUsesDidFlowWhenPasswordIsBlank() {
        User user = User.builder()
                .userId(1L)
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPassword("DID_ONLY:dentix123")
                .userPhoneNumber("01012345678")
                .findPwdQuestionId(1L)
                .findPwdAnswer("answer")
                .isVerify(YnType.Y)
                .daeguDid("did:mitum:minic:0x123")
                .daeguDidStatus(UserDaeguIdentityStatus.ISSUED)
                .build();

        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.of(user));
        given(jwtTokenUtil.createToken(user, TokenType.AccessToken)).willReturn("access-token");
        given(jwtTokenUtil.createToken(user, TokenType.RefreshToken)).willReturn("refresh-token");

        UserDto.LoginResponse response = userLoginService.userLogin(UserDto.LoginRequest.builder()
                .userLoginIdentifier("dentix123")
                .userPassword(null)
                .build());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getUserRefreshToken()).isEqualTo("refresh-token");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void userLoginUsesDidFlowEvenWhenPasswordIsPresent() {
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

        assertThatThrownBy(() -> userLoginService.userLogin(UserDto.LoginRequest.builder()
                .userLoginIdentifier("dentix123")
                .userPassword("wrong-password")
                .build()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("DID is not issued.");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
