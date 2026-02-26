package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementService;
import com.kaii.dentix.domain.agreement.dao.ServiceAgreementConsentRepository;
import com.kaii.dentix.domain.appService.dao.AppServiceRepository;
import com.kaii.dentix.domain.appService.dao.UserToAppServiceRepository;
import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.appService.domain.UserToAppService;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.*;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginService {

//    private final PatientRepository patientRepository;

    private final UserRepository userRepository;
    private final UserToAppServiceRepository userToAppServiceRepository;

    private final ServiceAgreementService serviceAgreementService;

    private final ServiceAgreementConsentRepository serviceAgreementConsentRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final FindPwdQuestionRepository findPwdQuestionRepository;

    private final PasswordEncoder passwordEncoder;

    private final ApplicationEventPublisher publisher;

    private final AdminRepository adminRepository;
    private final AppServiceRepository appServiceRepository;
    private final OrganizationRepository organizationRepository;
    private final ServiceAgreementConsentService serviceAgreementConsentService;

    /**
     * 사용자 회원 인증 (가입 여부 확인)
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDto.VerifyResponse userVerify(UserDto.VerifyRequest request) {
        List<User> users = userRepository.findByUserPhoneNumberOrUserName(
                request.getUserPhoneNumber(), request.getUserName());

        // 1. 이름 + 전화번호 일치 -> 이미 가입됨
        User exactUser = users.stream()
                .filter(u -> u.getUserName().equals(request.getUserName())
                        && u.getUserPhoneNumber().equals(request.getUserPhoneNumber()))
                .findFirst().orElse(null);

        if (exactUser != null) {
            throw new AlreadyDataException("이미 가입한 사용자입니다.");
        }

        // 2. 전화번호 중복 체크
        if (users.stream().anyMatch(u -> u.getUserPhoneNumber().equals(request.getUserPhoneNumber()))) {
            throw new BadRequestApiException("이미 사용중인 번호에요.\n번호를 다시 확인해 주세요.");
        }

        // 3. 이름 중복 체크 (선택 사항)
        if (users.stream().anyMatch(u -> u.getUserName().equals(request.getUserName()))) {
            throw new UnauthorizedException("회원 정보가 일치하지 않아요.\n다시 확인해 주세요.");
        }

        return new UserDto.VerifyResponse(null);
    }
    /**
     * 사용자 회원가입
     */
    @Transactional
    public UserDto.SignUpResponse userSignUp(UserDto.SignUpRequest request) {
        this.loginIdCheck(request.getUserLoginIdentifier());

        if (findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isEmpty()) {
            throw new NotFoundDataException("존재하지 않는 질문입니다.");
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        // 사용자 저장
        User user = userRepository.save(User.builder()
                .userLoginIdentifier(request.getUserLoginIdentifier())
                .userName(request.getUserName())
                .userGender(request.getUserGender())
                .userPassword(passwordEncoder.encode(request.getUserPassword()))
                .findPwdQuestionId(request.getFindPwdQuestionId())
                .findPwdAnswer(request.getFindPwdAnswer())
                .userPhoneNumber(request.getUserPhoneNumber())
                .organization(organization)
                .isVerify(YnType.N)
                .build());

        // 앱 서비스 연결
        List<AppService> appServices = appServiceRepository.findAllById(request.getAppServiceIds());
        if (appServices.isEmpty()) throw new NotFoundDataException("선택한 서비스가 존재하지 않습니다.");

        for (AppService appService : appServices) {
            userToAppServiceRepository.save(UserToAppService.builder().user(user).appService(appService).build());
        }

        // 토큰 발급
        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        user.updateLogin(refreshToken);

        // 약관 동의 저장
        serviceAgreementConsentService.saveUserServiceAgreements(
                user.getUserId(),
                request.getUserServiceAgreementRequest()
        );
//        this.saveServiceAgreements(request.getUserServiceAgreementRequest(), user.getUserId());

        return UserDto.SignUpResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userLoginIdentifier(user.getUserLoginIdentifier())
                .userName(user.getUserName())
                .userGender(user.getUserGender())
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .build();
    }

    /**
     * 사용자 로그인
     */
    @Transactional
    public UserDto.LoginResponse userLogin(UserDto.LoginRequest request) {
        User user = userRepository.findByUserLoginIdentifier(request.getUserLoginIdentifier())
                .orElseThrow(() -> new UnauthorizedException("아이디 혹은 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new UnauthorizedException("아이디 혹은 비밀번호가 올바르지 않습니다.");
        }

        if (user.getIsVerify() != YnType.Y) {
            throw new UnauthorizedException("관리자 승인 후 이용 가능합니다.");
        }

        // 구독/기관 정보
        Organization org = user.getOrganization();
        String planName = null;
        Boolean customSurvey = false;

        if (org != null && org.getOrganizationSubscription() != null) {
            SubscriptionPlan plan = org.getOrganizationSubscription().getSubscriptionPlan();
            if (plan != null) {
                planName = plan.getPlanName().name();
                customSurvey = plan.getCustomSurveyEnabled();
            }
        }

        // 토큰 발급
        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        user.updateLogin(refreshToken);

        // 서비스 목록
        List<UserDto.ServiceInfo> services = userToAppServiceRepository.findByUser(user).stream()
                .map(uta -> UserDto.ServiceInfo.builder()
                        .serviceId(uta.getAppService().getAppServiceId())
                        .name(uta.getAppService().getName())
                        .serviceType(uta.getAppService().getServiceType())
                        .build())
                .toList();

        Long mainServiceId = services.isEmpty() ? null : services.get(0).getServiceId();
        String mainServiceName = services.isEmpty() ? null : services.get(0).getName();

        return UserDto.LoginResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .serviceId(mainServiceId)
                .name(mainServiceName)
                .services(services)
                .organizationId(org != null ? org.getOrganizationId() : null)
                .organizationName(org != null ? org.getOrganizationName() : null)
                .organizationPlanName(planName)
                .organizationCustomSurveyEnabled(customSurvey)
                .build();
    }

    /**
     * 비밀번호 찾기 (질문/답변 확인)
     */
    @Transactional
    public UserDto.FindPasswordResponse userFindPassword(UserDto.FindPasswordRequest request) {
        User user = userRepository.findByUserLoginIdentifier(request.getUserLoginIdentifier())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 아이디입니다."));

        if (!user.getFindPwdQuestionId().equals(request.getFindPwdQuestionId()) ||
                !user.getFindPwdAnswer().equals(request.getFindPwdAnswer())) {
            throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
        }

        return UserDto.FindPasswordResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userLoginIdentifier(user.getUserLoginIdentifier())
                .build();
    }

    /**
     * 비밀번호 재설정 (비로그인 상태)
     */
    @Transactional
    public void userModifyPassword(Long userId, UserDto.ModifyPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 사용자입니다."));
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
    }

    /**
     * 아이디 중복 확인
     */
    @Transactional(readOnly = true)
    public void loginIdCheck(String userLoginIdentifier) {
        if (userRepository.findByUserLoginIdentifier(userLoginIdentifier).isPresent()) {
            throw new AlreadyDataException("이미 사용 중인 아이디입니다.");
        }
    }

    /**
     * AccessToken 재발급
     */
    public UserDto.AccessTokenResponse accessTokenReissue(HttpServletRequest request) {
        String refreshToken = jwtTokenUtil.getRefreshToken(request);

        if (jwtTokenUtil.isExpired(refreshToken, TokenType.RefreshToken)) throw new TokenExpiredException();
        Long roleId = jwtTokenUtil.getUserId(refreshToken, TokenType.RefreshToken);
        UserRole roles = jwtTokenUtil.getRoles(refreshToken, TokenType.RefreshToken);

        String newToken;
        if (roles == UserRole.ROLE_USER) {
            User user = userRepository.findById(roleId).orElseThrow(TokenExpiredException::new);
            if (StringUtils.isBlank(user.getUserRefreshToken()) || !user.getUserRefreshToken().equals(refreshToken))
                throw new UnauthorizedException();
            newToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        } else if (roles == UserRole.ROLE_ADMIN) {
            Admin admin = adminRepository.findById(roleId).orElseThrow(TokenExpiredException::new);
            if (StringUtils.isBlank(admin.getAdminRefreshToken()) || !admin.getAdminRefreshToken().equals(refreshToken))
                throw new UnauthorizedException();
            newToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        } else {
            throw new TokenExpiredException();
        }

        return new UserDto.AccessTokenResponse(newToken);
    }

//    // 내부 메서드: 서비스 약관 저장
//    private void saveServiceAgreements(List<Long> request, Long userId) {
//        List<ServiceAgreementDto.Response> list = serviceAgreementService.serviceAgreementList().getServiceAgreement();
//
//        if (request.stream().anyMatch(reqId -> list.stream().noneMatch(d -> d.getId().equals(reqId)))) {
//            throw new NotFoundDataException("존재하지 않는 서비스 이용 동의입니다.");
//        }
//
//        Date now = new Date();
//        list.forEach(agree -> {
//            if (agree.getIsServiceAgreeRequired() == YnType.Y && !request.contains(agree.getId())) {
//                throw new BadRequestApiException(agree.getName() + "는(은) 필수 항목입니다.");
//            }
//            userServiceAgreementRepository.save(UserServiceAgreement.builder()
//                    .userId(userId)
//                    .serviceAgreeId(agree.getId())
//                    .isUserServiceAgree(request.contains(agree.getId()) ? YnType.Y : YnType.N)
//                    .userServiceAgreeDate(now)
//                    .build());
//        });
//    }
}
