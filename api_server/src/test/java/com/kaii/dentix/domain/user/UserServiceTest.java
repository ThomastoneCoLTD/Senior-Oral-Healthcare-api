package com.kaii.dentix.domain.user;

import com.kaii.dentix.domain.agreement.dao.ServiceAgreementCustomRepository;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.appService.dao.AppServiceRepository;
import com.kaii.dentix.domain.appService.dao.UserToAppServiceRepository;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.TokenExpiredException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FindPwdQuestionRepository findPwdQuestionRepository;
    @Mock private ServiceAgreementRepository serviceAgreementRepository;
    @Mock private ServiceAgreementCustomRepository serviceAgreementCustomRepository;
    @Mock private AppServiceRepository appServiceRepository;
    @Mock private UserToAppServiceRepository userToAppServiceRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getTokenUser_throwsTokenExpiredExceptionWhenAccessTokenIsExpired() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);

        given(jwtTokenUtil.getAccessToken(request)).willReturn("expired-token");
        given(jwtTokenUtil.isExpired("expired-token", TokenType.AccessToken)).willReturn(true);

        assertThatThrownBy(() -> userService.getTokenUser(request))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void getTokenUser_throwsUnauthorizedExceptionWhenAccessTokenIsInvalid() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);

        given(jwtTokenUtil.getAccessToken(request)).willReturn("invalid-token");
        given(jwtTokenUtil.isExpired("invalid-token", TokenType.AccessToken)).willReturn(false);
        given(jwtTokenUtil.getRoles("invalid-token", TokenType.AccessToken)).willThrow(new RuntimeException("bad token"));

        assertThatThrownBy(() -> userService.getTokenUser(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증 정보가 올바르지 않습니다.");
    }

    @Test
    void getTokenUser_returnsUserWhenAccessTokenIsValid() {
        HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
        User user = User.builder().userId(1L).build();

        given(jwtTokenUtil.getAccessToken(request)).willReturn("valid-token");
        given(jwtTokenUtil.isExpired("valid-token", TokenType.AccessToken)).willReturn(false);
        given(jwtTokenUtil.getRoles("valid-token", TokenType.AccessToken)).willReturn(UserRole.ROLE_USER);
        given(jwtTokenUtil.getUserId("valid-token", TokenType.AccessToken)).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        org.assertj.core.api.Assertions.assertThat(userService.getTokenUser(request)).isEqualTo(user);
    }
}
