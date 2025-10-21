package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.appService.dao.AppServiceRepository;
import com.kaii.dentix.domain.appService.domain.AppService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.serviceAgreement.dto.ServiceAgreementDto;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
//import com.kaii.dentix.domain.patient.dao.PatientRepository;
//import com.kaii.dentix.domain.patient.domain.Patient;
import com.kaii.dentix.domain.serviceAgreement.application.ServiceAgreementService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.*;
import com.kaii.dentix.domain.user.dto.request.*;
import com.kaii.dentix.domain.user.event.UserModifyDeviceInfoEvent;
import com.kaii.dentix.domain.userServiceAgreement.dao.UserServiceAgreementRepository;
import com.kaii.dentix.domain.userServiceAgreement.domain.UserServiceAgreement;
import com.kaii.dentix.domain.userToAppService.dao.UserToAppServiceRepository;
import com.kaii.dentix.domain.userToAppService.domain.UserToAppService;
import com.kaii.dentix.global.common.error.exception.*;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserLoginService {

//    private final PatientRepository patientRepository;

    private final UserRepository userRepository;
    private final UserToAppServiceRepository userToAppServiceRepository;

    private final ServiceAgreementService serviceAgreementService;

    private final UserServiceAgreementRepository userServiceAgreementRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final FindPwdQuestionRepository findPwdQuestionRepository;

    private final PasswordEncoder passwordEncoder;

    private final ApplicationEventPublisher publisher;

    private final AdminRepository adminRepository;
    private final AppServiceRepository appServiceRepository;

    private final OrganizationRepository organizationRepository;
    /**
     * 사용자 서비스 이용동의 여부 확인 및 저장
     */
    public void userServiceAgreeCheckAndSave(List<Long> request, Long userId){
        List<ServiceAgreementDto> serviceAgreementList = serviceAgreementService.serviceAgreementList().getServiceAgreement();

        if (request.stream().anyMatch(serviceAgreementId -> serviceAgreementList.stream().noneMatch(requestId -> requestId.getId().equals(serviceAgreementId)))) {
            throw new NotFoundDataException("존재하지 않는 서비스 이용 동의입니다.");
        }

        Date now = new Date();

        serviceAgreementList.forEach(serviceAgreementDTO -> {
            if (serviceAgreementDTO.getIsServiceAgreeRequired().equals(YnType.Y) && !request.contains(serviceAgreementDTO.getId())) {
                throw new BadRequestApiException(serviceAgreementDTO.getName() + "는(은) 필수 항목입니다.");
            }

            userServiceAgreementRepository.save(UserServiceAgreement.builder()
                    .userId(userId)
                    .serviceAgreeId(serviceAgreementDTO.getId())
                    .isUserServiceAgree(request.contains(serviceAgreementDTO.getId()) ? YnType.Y : YnType.N)
                    .userServiceAgreeDate(now)
                    .build());
        });

    }

    /**
     *  사용자 회원 확인
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVerifyDto userVerify(UserVerifyRequest request){
        // 1. 이름 & 전화번호 모두 일치하는 유저 조회
        List<User> users = userRepository.findByUserPhoneNumberOrUserName(
                request.getUserPhoneNumber(),
                request.getUserName()
        );

        // 1. 이름 + 전화번호 모두 일치 → 이미 가입
        User exactUser = users.stream()
                .filter(u -> u.getUserName().equals(request.getUserName())
                        && u.getUserPhoneNumber().equals(request.getUserPhoneNumber()))
                .findFirst()
                .orElse(null);

        if (exactUser != null) {
            throw new AlreadyDataException("이미 가입한 사용자입니다.");
        }

        // 2. 전화번호만 일치 → 번호 중복
        boolean phoneExists = users.stream()
                .anyMatch(u -> u.getUserPhoneNumber().equals(request.getUserPhoneNumber()));

        if (phoneExists) {
            throw new BadRequestApiException("이미 사용중인 번호에요.\n번호를 다시 확인해 주세요.");
        }

        // 3. 이름만 일치 → 이름 중복 but 다른 번호
        boolean nameExists = users.stream()
                .anyMatch(u -> u.getUserName().equals(request.getUserName()));

        if (nameExists) {
            throw new UnauthorizedException("회원 정보가 일치하지 않아요.\n다시 확인해 주세요.");
        }

        // 4. 신규 사용자
        return UserVerifyDto.builder()
                .userId(null) // 아직 가입 전
                .build();
    }


    /**
     *  사용자 회원가입
     */
    @Transactional
    public UserSignUpDto userSignUp(HttpServletRequest httpServletRequest, UserSignUpRequest request) {

//        // ✅ 1. 인증된 회원인지 확인
//        if (request.getUserId() != null) {
//            userRepository.findById(request.getUserId())
//                    .orElseThrow(() -> new NotFoundDataException("존재하지 않는 회원입니다."));
//            if (userRepository.findByUserId(request.getUserId()).isPresent()) {
//                throw new AlreadyDataException("이미 가입한 사용자입니다.");
//            }
//        }

        // ✅ 2. 아이디 중복 확인
        this.loginIdCheck(request.getUserLoginIdentifier());

        // ✅ 3. 비밀번호 찾기 질문 유효성 검사
        if (findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isEmpty()) {
            throw new NotFoundDataException("존재하지 않는 질문입니다.");
        }
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));
        // ✅ 4. 사용자 정보 저장
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

        // ✅ 5. 서비스 정보 연결 (UserToAppService 생성)
        List<AppService> appServices = appServiceRepository.findAllById(request.getAppServiceIds());
        if (appServices.isEmpty()) {
            throw new NotFoundDataException("선택한 서비스가 존재하지 않습니다.");
        }

        for (AppService appService : appServices) {
            userToAppServiceRepository.save(UserToAppService.builder()
                    .user(user)
                    .appService(appService)
                    .build());
        }

        // ✅ 6. 토큰 생성 및 로그인 처리
        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        user.updateLogin(refreshToken);

        // ✅ 7. 서비스 이용 약관 동의 저장
        this.userServiceAgreeCheckAndSave(request.getUserServiceAgreementRequest(), user.getUserId());

        // ✅ 8. 디바이스 정보 이벤트 발행
        publisher.publishEvent(new UserModifyDeviceInfoEvent(user.getUserId(), httpServletRequest));

        // ✅ 9. 최종 반환 DTO
        return UserSignUpDto.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userLoginIdentifier(request.getUserLoginIdentifier())
                .userName(request.getUserName())
                .userGender(request.getUserGender())
                .organizationId(request.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .build();
    }
     /**
      * 아이디 중복 확인
     */
    @Transactional(readOnly = true)
    public void loginIdCheck(String userLoginIdentifier){

        if (userRepository.findByUserLoginIdentifier(userLoginIdentifier).isPresent()){
            throw new AlreadyDataException("이미 사용 중인 아이디입니다.");
        }

    }

    /**
     *  사용자 로그인
     */
    @Transactional
    public UserLoginDto userLogin(HttpServletRequest httpServletRequest, UserLoginRequest request) {

        // ✅ 사용자 조회
        User user = userRepository.findByUserLoginIdentifier(request.getUserLoginIdentifier())
                .orElseThrow(() -> new UnauthorizedException("아이디 혹은 비밀번호가 올바르지 않습니다."));

        // ✅ 비밀번호 확인
        if (!passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new UnauthorizedException("아이디 혹은 비밀번호가 올바르지 않습니다.");
        }

        // ✅ JWT 토큰 발급
        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);

        // ✅ 로그인 갱신
        user.updateLogin(refreshToken);

        // ✅ 디바이스 정보 이벤트 발행
        publisher.publishEvent(new UserModifyDeviceInfoEvent(
                user.getUserId(),
                httpServletRequest
        ));

        // ✅ 사용자의 서비스 목록 조회 (UserService → AppService)
        List<UserToAppService> mappings = userToAppServiceRepository.findByUser(user);

        List<UserLoginDto.AppServiceInfo> userServices = mappings.stream()
                .map(uta -> UserLoginDto.AppServiceInfo.builder()
                        .serviceId(uta.getAppService().getAppServiceId())
                        .name(uta.getAppService().getName())
                        .serviceType(uta.getAppService().getServiceType())
                        .build())
                .toList();

// ✅ 대표 서비스도 여전히 첫 번째로 유지
        Long mainServiceId = userServices.isEmpty() ? null : userServices.get(0).getServiceId();
        String mainServiceName = userServices.isEmpty() ? null : userServices.get(0).getName();

        return UserLoginDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .serviceId(mainServiceId)
                .name(mainServiceName)
                .services(userServices) // ✅ 전체 서비스 리스트 명시적으로 추가
                .build();
    }

    /**
     *  사용자 비밀번호 찾기
     */
    @Transactional
    public UserFindPasswordDto userFindPassword(UserFindPasswordRequest request){

        User user = userRepository.findByUserLoginIdentifier(request.getUserLoginIdentifier()).orElseThrow(() -> new NotFoundDataException("존재하지 않는 아이디입니다."));

        if (user.getFindPwdQuestionId().equals(request.getFindPwdQuestionId())){ // 입력받은 질문과 DB 정보가 일치하는 경우
            if (!user.getFindPwdAnswer().equals(request.getFindPwdAnswer())){ // 입력받은 답변과 DB 정보가 일치하지 않는 경우
                throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
            }
        } else { // 입력받은 질문과 DB 정보가 일치하지 않는 경우
            throw new UnauthorizedException("질문 혹은 답변이 일치하지 않습니다.");
        }

        return UserFindPasswordDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userLoginIdentifier(user.getUserLoginIdentifier())
                .build();

    }

    /**
     *  사용자 비밀번호 재설정
     */
    @Transactional
    public void userModifyPassword(UserModifyPasswordRequest request){
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new NotFoundDataException("존재하지 않는 회원입니다."));
        user.modifyUserPassword(passwordEncoder, request.getUserPassword());
    }

    /**
     *  AccessToken 재발급
     */
    public AccessTokenDto accessTokenReissue(HttpServletRequest request) {

        String refreshToken = jwtTokenUtil.getRefreshToken(request);

        if (jwtTokenUtil.isExpired(refreshToken, TokenType.RefreshToken)) throw new TokenExpiredException();
        Long roleId = jwtTokenUtil.getUserId(refreshToken, TokenType.RefreshToken);
        UserRole roles = jwtTokenUtil.getRoles(refreshToken, TokenType.RefreshToken);

        switch (roles) {
            case ROLE_USER:
                User user = userRepository.findById(roleId).orElseThrow(TokenExpiredException::new);
                if (StringUtils.isBlank(user.getUserRefreshToken()) || !user.getUserRefreshToken().equals(refreshToken))
                    throw new UnauthorizedException();

                return AccessTokenDto.builder().accessToken(jwtTokenUtil.createToken(user, TokenType.AccessToken)).build();
            case ROLE_ADMIN:
                Admin admin = adminRepository.findById(roleId).orElseThrow(TokenExpiredException::new);
                if (StringUtils.isBlank(admin.getAdminRefreshToken()) || !admin.getAdminRefreshToken().equals(refreshToken))
                    throw new UnauthorizedException();

                return AccessTokenDto.builder().accessToken(jwtTokenUtil.createToken(admin, TokenType.AccessToken)).build();

            default:
                throw new TokenExpiredException();
        }
    }

}
