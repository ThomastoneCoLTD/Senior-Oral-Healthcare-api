package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.agreement.dao.ServiceAgreementCustomRepository;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementRepository;
import com.kaii.dentix.domain.appService.dao.AppServiceRepository;
import com.kaii.dentix.domain.appService.dao.UserToAppServiceRepository;
import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.appService.domain.UserToAppService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final ApplicationEventPublisher publisher;
//    private final UserServiceAgreementRepository userServiceAgreementRepository;

    private final PasswordEncoder passwordEncoder;

    private final FindPwdQuestionRepository findPwdQuestionRepository;


    private final ServiceAgreementRepository serviceAgreementRepository;

//    private final PatientRepository patientRepository;

    private final ServiceAgreementCustomRepository serviceAgreementCustomRepository;
    private final AppServiceRepository appServiceRepository;
    private final UserToAppServiceRepository userToAppServiceRepository;

    /**
     * 토큰에서 User 추출
     */
    public User getTokenUser(HttpServletRequest request) {
        String token = jwtTokenUtil.getAccessToken(request);

        if (StringUtils.isBlank(token)) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        try {
            if (jwtTokenUtil.isExpired(token, TokenType.AccessToken)) {
                throw new TokenExpiredException();
            }

            if (jwtTokenUtil.getRoles(token, TokenType.AccessToken) != UserRole.ROLE_USER) {
                throw new UnauthorizedException("권한이 없는 사용자입니다.");
            }

            Long userId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));
        } catch (TokenExpiredException | UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("인증 정보가 올바르지 않습니다.");
        }
    }

    /**
     * 사용자 자동 로그인 (토큰 갱신)
     */
    @Transactional
    public UserDto.TokenResponse userAutoLogin(HttpServletRequest request) {
        User user = this.getTokenUser(request);

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        userRepository.updateLoginInfo(user.getUserId(), refreshToken, new Date());

        publisher.publishEvent(new UserModifyDeviceInfoEvent(user.getUserId(), request));

        return new UserDto.TokenResponse(accessToken, refreshToken);
    }


    /**
     * 토큰에서 User 추출 - 토큰 NULL 허용
     */
    public User getTokenUserNullable(HttpServletRequest servletRequest) {

        String token = jwtTokenUtil.getAccessToken(servletRequest);
        log.info("Authorization header = {}", servletRequest.getHeader("Authorization"));

        if (StringUtils.isBlank(token)){ // 비로그인 사용자
            return null;
        }

        if (jwtTokenUtil.isExpired(token, TokenType.AccessToken)) throw new TokenExpiredException();

        UserRole roles = jwtTokenUtil.getRoles(token, TokenType.AccessToken);
        if (!roles.equals(UserRole.ROLE_USER)) throw new UnauthorizedException("권한이 없는 사용자입니다.");

        Long userId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));

    }


    /**
     *  사용자 자동 로그인
     */
    @Transactional
    public UserLoginDto userAutoLogin(HttpServletRequest httpServletRequest, UserAutoLoginRequest userAutoLoginRequest){
        User user = this.getTokenUser(httpServletRequest);

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);

        user.updateLogin(refreshToken);

        publisher.publishEvent(new UserModifyDeviceInfoEvent(
                user.getUserId(),
                httpServletRequest

        ));

        return UserLoginDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 사용자 비밀번호 확인
     */
    @Transactional
    public void userPasswordVerify(HttpServletRequest httpServletRequest, UserDto.PasswordVerifyRequest request) {
        User user = this.getTokenUser(httpServletRequest);

        if (!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())){
            throw new UnauthorizedException("비밀번호가 일치하지 않습니다.");
        }
    }

    /**
     *  사용자 보안정보수정 - 비밀번호 변경
     */
    @Transactional
    public void userModifyPassword(HttpServletRequest httpServletRequest, UserDto.ModifyPasswordRequest request){
        User user = this.getTokenUser(httpServletRequest);
        user.modifyUserPassword(passwordEncoder, request.getUserPassword());
    }

    /**
     * 사용자 보안정보수정 - 질문과 답변 수정
     */
    @Transactional
    public UserDto.ModifyQnAResponse userModifyQnA(HttpServletRequest httpServletRequest, UserDto.ModifyQnARequest request) {
        User user = this.getTokenUser(httpServletRequest);

        // 올바르지 않은 findPwdQuestionId 인 경우
        if (!findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isPresent()) {
            throw new NotFoundDataException("존재하지 않는 질문입니다.");
        }

        user.modifyQnA(request.getFindPwdQuestionId(), request.getFindPwdAnswer());

        return UserDto.ModifyQnAResponse.builder()
                .findPwdQuestionId(user.getFindPwdQuestionId())
                .findPwdAnswer(user.getFindPwdAnswer())
                .build();
    }

    /**
     * 사용자 회원 정보 수정
     */
    @Transactional
    public UserDto.InfoResponse userModifyInfo(HttpServletRequest httpServletRequest, UserDto.ModifyInfoRequest request) {
        User user = this.getTokenUser(httpServletRequest);

        // 정보 수정
        user.modifyInfo(request.getUserName(), request.getUserGender());

        // 통합 DTO인 UserDto.InfoResponse 로 반환
        return UserDto.InfoResponse.builder()
                .userName(user.getUserName())
                .userGender(user.getUserGender())
                .build();
    }




    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserDto.InfoResponse getUserInfo(HttpServletRequest request) {
        User user = this.getTokenUser(request);

        // Fetch Join 사용 권장
        User fullUser = userRepository.findByUserIdWithServices(user.getUserId())
                .orElseThrow(() -> new NotFoundDataException("사용자를 찾을 수 없습니다."));

        List<UserDto.ServiceInfo> services = userToAppServiceRepository.findByUser(fullUser).stream()
                .map(rel -> UserDto.ServiceInfo.builder()
                        .serviceId(rel.getAppService().getAppServiceId())
                        .name(rel.getAppService().getName())
                        .serviceType(rel.getAppService().getServiceType())
                        .build())
                .toList();

        return UserDto.InfoResponse.builder()
                .userName(fullUser.getUserName())
                .userLoginIdentifier(fullUser.getUserLoginIdentifier())
                .userGender(fullUser.getUserGender())
                .services(services)
                .build();
    }

    /**
     *  사용자 로그아웃
     */
    @Transactional
    public void userLogout(HttpServletRequest httpServletRequest){
        User user = this.getTokenUser(httpServletRequest);
        user.logout();
    }

    /**
     *  사용자 회원탈퇴
     */
    @Transactional
    public void userRevoke(HttpServletRequest httpServletRequest){
        User user = this.getTokenUser(httpServletRequest);
        user.revoke();
    }

    /**
     *사용자 서비스 업데이트
     */
    @Transactional
    public UserDto.ServiceUpdateResponse updateUserServices(HttpServletRequest httpServletRequest, UserDto.ServiceUpdateRequest request) {
        User user = this.getTokenUser(httpServletRequest);
        List<UserToAppService> currentRelations = userToAppServiceRepository.findByUser(user);

        Set<Long> newServiceIds = new HashSet<>(request.getServiceIds());

        // 현재 연결된 서비스 ID
        Set<Long> existingIds = currentRelations.stream()
                .map(rel -> rel.getAppService().getAppServiceId())   // ★ 수정됨
                .collect(Collectors.toSet());

        // 추가해야 할 서비스
        Set<Long> toAdd = newServiceIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toSet());

        // 삭제해야 할 서비스
        Set<Long> toRemove = existingIds.stream()
                .filter(id -> !newServiceIds.contains(id))
                .collect(Collectors.toSet());

        // 삭제 처리
        if (!toRemove.isEmpty()) {
            userToAppServiceRepository.deleteAll(
                    currentRelations.stream()
                            .filter(rel -> toRemove.contains(rel.getAppService().getAppServiceId())) // ★ 수정됨
                            .toList()
            );
        }

        // 추가 처리
        for (Long id : toAdd) {
            AppService appService = appServiceRepository.findById(id)
                    .orElseThrow(() -> new NotFoundDataException("존재하지 않는 서비스입니다."));

            userToAppServiceRepository.save(
                    UserToAppService.builder()
                            .user(user)
                            .appService(appService)
                            .build()
            );
        }

        // 최신 목록 반환
        List<UserDto.ServiceInfo> services = userToAppServiceRepository.findByUser(user).stream()
                .map(rel -> UserDto.ServiceInfo.builder()
                        .serviceId(rel.getAppService().getAppServiceId())
                        .name(rel.getAppService().getName())
                        .serviceType(rel.getAppService().getServiceType())
                        .build())
                .toList();

        return UserDto.ServiceUpdateResponse.builder()
                .userName(user.getUserName())
                .services(services)
                .build();
    }
}
