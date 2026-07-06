package com.kaii.dentix.domain.user;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.application.DaeguDefaultOrganizationService;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserDaeguProvisioningService;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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

    @InjectMocks
    private UserLoginService userLoginService;

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
                .daeguCredentialJwt("jwt")
                .build();

        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.of(user));
        given(daeguChainDidService.verifyLoginUserCredential(user)).willReturn(true);
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

    @Test
    void userLoginReissuesLoginCredentialWhenStoredCredentialCannotBeVerified() {
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
                .daeguCredentialJwt("old-or-non-login-credential")
                .build();

        given(userRepository.findByUserLoginIdentifier("dentix123")).willReturn(Optional.of(user));
        given(daeguChainDidService.verifyLoginUserCredential(user)).willReturn(false, true);
        given(jwtTokenUtil.createToken(user, TokenType.AccessToken)).willReturn("access-token");
        given(jwtTokenUtil.createToken(user, TokenType.RefreshToken)).willReturn("refresh-token");

        UserDto.LoginResponse response = userLoginService.userDidLogin(UserDto.DidLoginRequest.builder()
                .userLoginIdentifier("dentix123")
                .build());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(daeguChainDidService).issueLoginUserCredential(user);
        verify(daeguChainDidService, times(2)).verifyLoginUserCredential(user);
    }
}
