package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.domain.user.dto.UserLoginDto;
import com.kaii.dentix.domain.user.dto.request.UserAutoLoginRequest;
import com.kaii.dentix.domain.user.event.UserModifyDeviceInfoEvent;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.TokenExpiredException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;
    private final FindPwdQuestionRepository findPwdQuestionRepository;

    public User getTokenUser(HttpServletRequest request) {
        String token = jwtTokenUtil.getAccessToken(request);

        if (StringUtils.isBlank(token)) {
            throw new UnauthorizedException("Authentication information is missing.");
        }

        try {
            if (jwtTokenUtil.isExpired(token, TokenType.AccessToken)) {
                throw new TokenExpiredException();
            }

            if (jwtTokenUtil.getRoles(token, TokenType.AccessToken) != UserRole.ROLE_USER) {
                throw new UnauthorizedException("User permission is required.");
            }

            Long userId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundDataException("User does not exist."));
        } catch (TokenExpiredException | UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("Invalid authentication information.");
        }
    }

    @Transactional
    public UserDto.TokenResponse userAutoLogin(HttpServletRequest request) {
        User user = this.getTokenUser(request);

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        userRepository.updateLoginInfo(user.getUserId(), refreshToken, new Date());

        publisher.publishEvent(new UserModifyDeviceInfoEvent(user.getUserId(), request));

        return new UserDto.TokenResponse(accessToken, refreshToken);
    }

    public User getTokenUserNullable(HttpServletRequest servletRequest) {
        String token = jwtTokenUtil.getAccessToken(servletRequest);
        log.info("Authorization header = {}", servletRequest.getHeader("Authorization"));

        if (StringUtils.isBlank(token)) {
            return null;
        }

        if (jwtTokenUtil.isExpired(token, TokenType.AccessToken)) {
            throw new TokenExpiredException();
        }

        UserRole roles = jwtTokenUtil.getRoles(token, TokenType.AccessToken);
        if (!roles.equals(UserRole.ROLE_USER)) {
            throw new UnauthorizedException("User permission is required.");
        }

        Long userId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
        return userRepository.findByIdWithOrganizationAndSubscription(userId)
                .orElseThrow(() -> new NotFoundDataException("User does not exist."));
    }

    @Transactional
    public UserLoginDto userAutoLogin(HttpServletRequest httpServletRequest, UserAutoLoginRequest userAutoLoginRequest) {
        User user = this.getTokenUser(httpServletRequest);

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);

        user.updateLogin(refreshToken);
        publisher.publishEvent(new UserModifyDeviceInfoEvent(user.getUserId(), httpServletRequest));

        return UserLoginDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void userPasswordVerify(HttpServletRequest httpServletRequest, UserDto.PasswordVerifyRequest request) {
        User user = this.getTokenUser(httpServletRequest);

        if (!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new UnauthorizedException("Password does not match.");
        }
    }

    @Transactional
    public void userModifyPassword(HttpServletRequest httpServletRequest, UserDto.ModifyPasswordRequest request) {
        User user = this.getTokenUser(httpServletRequest);
        user.modifyUserPassword(passwordEncoder, request.getUserPassword());
    }

    @Transactional
    public UserDto.ModifyQnAResponse userModifyQnA(HttpServletRequest httpServletRequest, UserDto.ModifyQnARequest request) {
        User user = this.getTokenUser(httpServletRequest);

        if (findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isEmpty()) {
            throw new NotFoundDataException("Password question does not exist.");
        }

        user.modifyQnA(request.getFindPwdQuestionId(), request.getFindPwdAnswer());

        return UserDto.ModifyQnAResponse.builder()
                .findPwdQuestionId(user.getFindPwdQuestionId())
                .findPwdAnswer(user.getFindPwdAnswer())
                .build();
    }

    @Transactional
    public UserDto.InfoResponse userModifyInfo(HttpServletRequest httpServletRequest, UserDto.ModifyInfoRequest request) {
        User user = this.getTokenUser(httpServletRequest);
        user.modifyInfo(request.getUserName(), request.getUserGender());

        return UserDto.InfoResponse.builder()
                .userName(user.getUserName())
                .userGender(user.getUserGender())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto.InfoResponse getUserInfo(HttpServletRequest request) {
        User user = this.getTokenUser(request);

        return UserDto.InfoResponse.builder()
                .userName(user.getUserName())
                .userLoginIdentifier(user.getUserLoginIdentifier())
                .userGender(user.getUserGender())
                .services(UserDto.defaultServiceInfo())
                .daeguDid(user.getDaeguDid())
                .daeguDidStatus(user.getDaeguDidStatus())
                .build();
    }

    @Transactional
    public void userLogout(HttpServletRequest httpServletRequest) {
        User user = this.getTokenUser(httpServletRequest);
        user.logout();
    }

    @Transactional
    public void userRevoke(HttpServletRequest httpServletRequest) {
        User user = this.getTokenUser(httpServletRequest);
        user.revoke();
    }
}
