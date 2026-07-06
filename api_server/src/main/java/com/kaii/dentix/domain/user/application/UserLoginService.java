package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.findPwdQuestion.dao.FindPwdQuestionRepository;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.organization.application.DaeguDefaultOrganizationService;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.TokenExpiredException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final FindPwdQuestionRepository findPwdQuestionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;
    private final DaeguDefaultOrganizationService daeguDefaultOrganizationService;
    private final ServiceAgreementConsentService serviceAgreementConsentService;
    private final UserDaeguProvisioningService userDaeguProvisioningService;
    private final DaeguChainDidService daeguChainDidService;

    @Transactional(readOnly = true)
    public UserDto.VerifyResponse userPhoneCheck(String userPhoneNumber) {
        String normalizedPhoneNumber = normalizePhoneNumber(userPhoneNumber);
        return userRepository.findByUserPhoneNumber(normalizedPhoneNumber)
                .map(user -> UserDto.VerifyResponse.builder()
                        .userId(user.getUserId())
                        .build())
                .orElseGet(() -> UserDto.VerifyResponse.builder()
                        .userId(null)
                        .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDto.VerifyResponse userVerify(UserDto.VerifyRequest request) {
        String normalizedPhoneNumber = normalizePhoneNumber(request.getUserPhoneNumber());
        String userName = request.getUserName().trim();
        List<User> users = userRepository.findByUserPhoneNumberOrUserName(normalizedPhoneNumber, userName);

        User exactUser = users.stream()
                .filter(user -> user.getUserName().equals(userName)
                        && user.getUserPhoneNumber().equals(normalizedPhoneNumber))
                .findFirst()
                .orElse(null);

        if (exactUser != null) {
            throw new AlreadyDataException("Already registered user.");
        }

        if (users.stream().anyMatch(user -> user.getUserPhoneNumber().equals(normalizedPhoneNumber))) {
            throw new BadRequestApiException("Already registered phone number.");
        }

        if (users.stream().anyMatch(user -> user.getUserName().equals(userName))) {
            throw new UnauthorizedException("User information does not match.");
        }

        return new UserDto.VerifyResponse(null);
    }

    @Transactional
    public UserDto.SignUpResponse userSignUp(UserDto.SignUpRequest request) {
        this.loginIdCheck(request.getUserLoginIdentifier());
        String userPhoneNumber = normalizePhoneNumber(request.getUserPhoneNumber());
        assertPhoneNumberAvailable(userPhoneNumber);

        if (findPwdQuestionRepository.findById(request.getFindPwdQuestionId()).isEmpty()) {
            throw new NotFoundDataException("Password question does not exist.");
        }

        Organization organization = daeguDefaultOrganizationService.getOrCreate();
        User user = userRepository.save(User.builder()
                .userLoginIdentifier(request.getUserLoginIdentifier())
                .userName(request.getUserName())
                .userGender(request.getUserGender())
                .userPassword(passwordEncoder.encode(request.getUserPassword()))
                .findPwdQuestionId(request.getFindPwdQuestionId())
                .findPwdAnswer(request.getFindPwdAnswer())
                .userPhoneNumber(userPhoneNumber)
                .organization(organization)
                .isVerify(YnType.Y)
                .build());

        userDaeguProvisioningService.provisionForSignUp(user);

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        user.updateLogin(refreshToken);

        serviceAgreementConsentService.saveUserServiceAgreements(
                user.getUserId(),
                request.getUserServiceAgreementRequest()
        );

        return buildSignUpResponse(user, organization, accessToken, refreshToken);
    }

    @Transactional
    public UserDto.SignUpResponse userDidSignUp(UserDto.DidSignUpRequest request) {
        this.loginIdCheck(request.getUserLoginIdentifier());
        String userPhoneNumber = normalizePhoneNumber(request.getUserPhoneNumber());
        assertPhoneNumberAvailable(userPhoneNumber);

        Organization organization = daeguDefaultOrganizationService.getOrCreate();
        String loginIdentifier = request.getUserLoginIdentifier().trim();

        User user = userRepository.save(User.builder()
                .userLoginIdentifier(loginIdentifier)
                .userName(request.getUserName())
                .userGender(GenderType.M)
                .userPassword(passwordEncoder.encode("DID_ONLY:" + loginIdentifier))
                .findPwdQuestionId(resolveDefaultFindPwdQuestionId())
                .findPwdAnswer(loginIdentifier)
                .userPhoneNumber(userPhoneNumber)
                .userBirthDate(request.getUserBirthDate())
                .organization(organization)
                .isVerify(YnType.Y)
                .build());

        userDaeguProvisioningService.provisionForSignUp(user);

        serviceAgreementConsentService.saveUserServiceAgreements(
                user.getUserId(),
                request.getUserServiceAgreementRequest()
        );

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        user.updateLogin(refreshToken);

        return buildSignUpResponse(user, organization, accessToken, refreshToken);
    }

    @Transactional
    public UserDto.LoginResponse userLogin(UserDto.LoginRequest request) {
        return userDidLogin(UserDto.DidLoginRequest.builder()
                .userLoginIdentifier(request.getUserLoginIdentifier())
                .build());
    }

    @Transactional
    public UserDto.LoginResponse userDidLogin(UserDto.DidLoginRequest request) {
        User user = userRepository.findByUserLoginIdentifier(request.getUserLoginIdentifier())
                .orElseThrow(() -> new UnauthorizedException("Invalid login identifier or DID."));

        if (user.getIsVerify() != YnType.Y) {
            throw new UnauthorizedException("User is not verified.");
        }
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED || StringUtils.isBlank(user.getDaeguDid())) {
            throw new UnauthorizedException("DID is not issued.");
        }
        verifyDaeguCredentialForLogin(user);

        String accessToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        String refreshToken = jwtTokenUtil.createToken(user, TokenType.RefreshToken);
        user.updateLogin(refreshToken);

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    private UserDto.SignUpResponse buildSignUpResponse(
            User user,
            Organization organization,
            String accessToken,
            String refreshToken
    ) {
        return UserDto.SignUpResponse.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userLoginIdentifier(user.getUserLoginIdentifier())
                .userName(user.getUserName())
                .userGender(user.getUserGender())
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .daeguDid(user.getDaeguDid())
                .daeguDidStatus(user.getDaeguDidStatus())
                .daeguCredentialStatus(user.getDaeguCredentialStatus())
                .build();
    }

    private UserDto.LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
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

        List<UserDto.ServiceInfo> services = UserDto.defaultServiceInfo();
        Long mainServiceId = services.get(0).getServiceId();
        String mainServiceName = services.get(0).getName();

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
                .daeguDid(user.getDaeguDid())
                .daeguDidStatus(user.getDaeguDidStatus())
                .build();
    }

    private void verifyDaeguCredentialForLogin(User user) {
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED || StringUtils.isBlank(user.getDaeguDid())) {
            throw new UnauthorizedException("DID credential is not issued.");
        }

        if (StringUtils.isBlank(user.getDaeguCredentialJwt())) {
            issueLoginCredentialOrThrow(user);
        }

        if (daeguChainDidService.verifyLoginUserCredential(user)) {
            return;
        }

        issueLoginCredentialOrThrow(user);
        if (!daeguChainDidService.verifyLoginUserCredential(user)) {
            throw new UnauthorizedException("DID credential verification failed.");
        }
    }

    private void issueLoginCredentialOrThrow(User user) {
        try {
            daeguChainDidService.issueLoginUserCredential(user);
        } catch (RuntimeException exception) {
            user.markDaeguCredentialFailed();
            throw new UnauthorizedException("DID credential verification failed.");
        }
    }

    private Long resolveDefaultFindPwdQuestionId() {
        return findPwdQuestionRepository.findAll().stream()
                .findFirst()
                .map(question -> question.getFindPwdQuestionId())
                .orElse(1L);
    }

    @Transactional(readOnly = true)
    public void loginIdCheck(String userLoginIdentifier) {
        if (userRepository.findByUserLoginIdentifier(userLoginIdentifier).isPresent()) {
            throw new AlreadyDataException("Already registered login identifier.");
        }
    }

    private void assertPhoneNumberAvailable(String userPhoneNumber) {
        if (userRepository.findByUserPhoneNumber(userPhoneNumber).isPresent()) {
            throw new AlreadyDataException("Already registered phone number.");
        }
    }

    private String normalizePhoneNumber(String userPhoneNumber) {
        if (StringUtils.isBlank(userPhoneNumber)) {
            throw new BadRequestApiException("Phone number is required.");
        }

        String normalizedPhoneNumber = userPhoneNumber.replaceAll("[^0-9]", "");
        if (!normalizedPhoneNumber.matches("^[0-9]{10,11}$")) {
            throw new BadRequestApiException("Phone number must be 10 to 11 digits.");
        }

        return normalizedPhoneNumber;
    }

    public UserDto.AccessTokenResponse accessTokenReissue(HttpServletRequest request) {
        String refreshToken = jwtTokenUtil.getRefreshToken(request);

        if (jwtTokenUtil.isExpired(refreshToken, TokenType.RefreshToken)) {
            throw new TokenExpiredException();
        }
        Long roleId = jwtTokenUtil.getUserId(refreshToken, TokenType.RefreshToken);
        UserRole roles = jwtTokenUtil.getRoles(refreshToken, TokenType.RefreshToken);

        String newToken;
        if (roles == UserRole.ROLE_USER) {
            User user = userRepository.findById(roleId).orElseThrow(TokenExpiredException::new);
            if (StringUtils.isBlank(user.getUserRefreshToken()) || !user.getUserRefreshToken().equals(refreshToken)) {
                throw new UnauthorizedException();
            }
            newToken = jwtTokenUtil.createToken(user, TokenType.AccessToken);
        } else if (roles == UserRole.ROLE_ADMIN) {
            Admin admin = adminRepository.findById(roleId).orElseThrow(TokenExpiredException::new);
            if (StringUtils.isBlank(admin.getAdminRefreshToken()) || !admin.getAdminRefreshToken().equals(refreshToken)) {
                throw new UnauthorizedException();
            }
            newToken = jwtTokenUtil.createToken(admin, TokenType.AccessToken);
        } else {
            throw new TokenExpiredException();
        }

        return new UserDto.AccessTokenResponse(newToken);
    }
}
