package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.config.DadaeguLoginProperties;
import com.kaii.dentix.domain.user.dao.DadaeguUserIdentityRepository;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.DadaeguSignupSession;
import com.kaii.dentix.domain.user.domain.DadaeguUserIdentity;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DadaeguLoginServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DadaeguLoginProperties properties;
    private UserRepository userRepository;
    private DadaeguUserIdentityRepository dadaeguUserIdentityRepository;
    private DadaeguSignupSessionService signupSessionService;
    private UserLoginService userLoginService;
    private UserDaeguProvisioningService userDaeguProvisioningService;
    private DadaeguLoginService service;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        properties = new DadaeguLoginProperties();
        properties.setEnabled(true);
        properties.setSiteId("soh-test-site");
        properties.setRsaPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        userRepository = mock(UserRepository.class);
        dadaeguUserIdentityRepository = mock(DadaeguUserIdentityRepository.class);
        signupSessionService = mock(DadaeguSignupSessionService.class);
        userLoginService = mock(UserLoginService.class);
        userDaeguProvisioningService = mock(UserDaeguProvisioningService.class);
        service = new DadaeguLoginService(
                properties, userRepository, dadaeguUserIdentityRepository,
                signupSessionService, userLoginService, userDaeguProvisioningService, objectMapper
        );
    }

    @Test
    void configExposesOnlyPublicValuesWhenIntegrationIsReady() {
        UserDto.DadaeguLoginConfigResponse response = service.getConfig();
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getSiteId()).isEqualTo("soh-test-site");
        assertThat(response.getRequiredVc()).isEqualTo("DaeguMasterVC");
    }

    @Test
    void loginDecryptsMasterVcAndMatchesExistingLocalAccount() throws Exception {
        User user = User.builder()
                .userId(31L)
                .userLoginIdentifier("local-user")
                .userName("홍길동")
                .userPhoneNumber("01012345678")
                .userBirthDate("1950-01-02")
                .isVerify(YnType.Y)
                .build();
        UserDto.LoginResponse expected = UserDto.LoginResponse.builder()
                .userId(31L).accessToken("access-token").build();
        when(dadaeguUserIdentityRepository.findByExternalDid("did:daegu:test-user"))
                .thenReturn(Optional.empty());
        when(dadaeguUserIdentityRepository.findByCiHash(hash("ci:test-user")))
                .thenReturn(Optional.empty());
        when(userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                "01012345678", "홍길동", "1950-01-02"
        )).thenReturn(Optional.of(user));
        when(dadaeguUserIdentityRepository.findByUserId(31L)).thenReturn(Optional.empty());
        when(userLoginService.completeAuthenticatedLogin(user)).thenReturn(expected);

        UserDto.LoginResponse response = service.login(encryptedLoginRequest(
                "did:daegu:test-user", "ci:test-user", "홍길동", "19500102", "010-1234-5678", "M"
        ));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        ArgumentCaptor<DadaeguUserIdentity> identityCaptor = ArgumentCaptor.forClass(DadaeguUserIdentity.class);
        verify(dadaeguUserIdentityRepository).saveAndFlush(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getCiHash())
                .isEqualTo(hash("ci:test-user"))
                .isNotEqualTo("ci:test-user");
        verify(userDaeguProvisioningService).provisionForDadaegu(user, "did:daegu:test-user");
        verify(userLoginService).completeAuthenticatedLogin(user);
    }

    @Test
    void firstLoginReturnsShortLivedOnboardingToken() throws Exception {
        when(dadaeguUserIdentityRepository.findByExternalDid("did:daegu:new-user"))
                .thenReturn(Optional.empty());
        when(dadaeguUserIdentityRepository.findByCiHash(hash("ci:new-user")))
                .thenReturn(Optional.empty());
        when(userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                "01099998888", "신규사용자", "1960-03-04"
        )).thenReturn(Optional.empty());
        when(signupSessionService.issue(
                "did:daegu:new-user", hash("ci:new-user"), "신규사용자",
                "01099998888", "1960-03-04", GenderType.W
        )).thenReturn(new DadaeguSignupSessionService.IssueResult("onboarding-token", 600));

        UserDto.LoginResponse response = service.login(encryptedLoginRequest(
                "did:daegu:new-user", "ci:new-user", "신규사용자", "19600304", "010-9999-8888", "F"
        ));

        assertThat(response.getDadaeguOnboardingRequired()).isTrue();
        assertThat(response.getDadaeguOnboardingToken()).isEqualTo("onboarding-token");
        assertThat(response.getDadaeguOnboardingExpiresInSeconds()).isEqualTo(600);
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    void completeSignUpCreatesDadaeguOnlyUserAndLogsIn() {
        DadaeguSignupSession session = DadaeguSignupSession.builder()
                .externalDid("did:daegu:new-user").ciHash(hash("ci:new-user")).userName("신규사용자")
                .userPhoneNumber("01099998888").userBirthDate("1960-03-04")
                .userGender(GenderType.W).build();
        User user = User.builder()
                .userId(41L).userLoginIdentifier("dg-generated").isVerify(YnType.Y).build();
        UserDto.LoginResponse expected = UserDto.LoginResponse.builder()
                .userId(41L).accessToken("access-token").build();
        when(signupSessionService.consume("onboarding-token")).thenReturn(session);
        when(dadaeguUserIdentityRepository.findByExternalDid("did:daegu:new-user"))
                .thenReturn(Optional.empty());
        when(dadaeguUserIdentityRepository.findByCiHash(hash("ci:new-user")))
                .thenReturn(Optional.empty());
        when(userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                "01099998888", "신규사용자", "1960-03-04"
        )).thenReturn(Optional.empty());
        when(userRepository.findByUserLoginIdentifier(anyString())).thenReturn(Optional.empty());
        when(userLoginService.createDadaeguUser(
                anyString(), eq("신규사용자"), eq(GenderType.W), eq("01099998888"),
                eq("1960-03-04"), eq("대구2"), eq(List.of(1L, 2L)), eq(true)
        )).thenReturn(user);
        when(dadaeguUserIdentityRepository.findByUserId(41L)).thenReturn(Optional.empty());
        when(userLoginService.completeAuthenticatedLogin(user)).thenReturn(expected);

        UserDto.LoginResponse response = service.completeSignUp(UserDto.DadaeguSignUpRequest.builder()
                .onboardingToken("onboarding-token")
                .realOrganization("대구2")
                .userServiceAgreementRequest(List.of(1L, 2L))
                .oralAnalysisServiceEnabled(true)
                .build());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(dadaeguUserIdentityRepository).saveAndFlush(any());
        verify(userDaeguProvisioningService).provisionForDadaegu(user, "did:daegu:new-user");
        verify(userLoginService).completeAuthenticatedLogin(user);
    }

    @Test
    void loginUsesCiMappingBeforePersonalInformationFallback() throws Exception {
        User user = User.builder()
                .userId(51L)
                .userLoginIdentifier("ci-user")
                .isVerify(YnType.Y)
                .build();
        DadaeguUserIdentity identity = DadaeguUserIdentity.builder()
                .userId(51L)
                .externalDid("did:daegu:old-ci-user")
                .ciHash(hash("ci:stable-user"))
                .build();
        when(dadaeguUserIdentityRepository.findByExternalDid("did:daegu:ci-user"))
                .thenReturn(Optional.empty());
        when(dadaeguUserIdentityRepository.findByCiHash(hash("ci:stable-user")))
                .thenReturn(Optional.of(identity));
        when(userRepository.findById(51L)).thenReturn(Optional.of(user));
        when(userLoginService.completeAuthenticatedLogin(user))
                .thenReturn(UserDto.LoginResponse.builder().userId(51L).accessToken("access").build());

        UserDto.LoginResponse response = service.login(encryptedLoginRequest(
                "did:daegu:ci-user", "ci:stable-user", "변경된이름",
                "19600304", "010-1111-2222", "F"
        ));

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(identity.getExternalDid()).isEqualTo("did:daegu:ci-user");
        verify(userRepository, never()).findByUserPhoneNumberAndUserNameAndUserBirthDate(
                anyString(), anyString(), anyString()
        );
    }

    @Test
    void loginIsRejectedUntilServerCredentialsAreConfigured() {
        properties.setRsaPrivateKey("");
        ObjectNode callback = objectMapper.createObjectNode().put("did", "anything");
        assertThatThrownBy(() -> service.login(
                UserDto.DadaeguLoginRequest.builder().encryptedData(callback).build()
        )).isInstanceOf(BadRequestApiException.class).hasMessageContaining("연동 정보");
    }

    @Test
    void encryptedCallbackPayloadIsMaskedDuringRequestLogging() throws Exception {
        ObjectNode encrypted = objectMapper.createObjectNode().put("did", "secret-ciphertext");
        UserDto.DadaeguLoginRequest request = UserDto.DadaeguLoginRequest.builder()
                .encryptedData(encrypted).build();
        assertThat(objectMapper.writeValueAsString(request))
                .isEqualTo("{\"encryptedData\":\"********\"}");
    }

    @Test
    void onboardingTokenIsMaskedDuringRequestLogging() throws Exception {
        UserDto.DadaeguSignUpRequest request = UserDto.DadaeguSignUpRequest.builder()
                .onboardingToken("secret-onboarding-token")
                .realOrganization("대구1")
                .userServiceAgreementRequest(List.of(1L))
                .oralAnalysisServiceEnabled(false).build();
        assertThat(objectMapper.writeValueAsString(request))
                .contains("\"onboardingToken\":\"********\"")
                .doesNotContain("secret-onboarding-token");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "M", "m", "MALE", "MAN", "남", "남성", "남자",
            "1", "3", "5", "7", "\"M\"", "male (M)"
    })
    void normalizesDadaeguMaleValues(String gender) {
        assertThat((GenderType) ReflectionTestUtils.invokeMethod(service, "normalizeGender", gender))
                .isEqualTo(GenderType.M);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "W", "F", "f", "FEMALE", "WOMAN", "여", "여성", "여자",
            "2", "4", "6", "8", "\"F\"", "female (F)"
    })
    void normalizesDadaeguFemaleValues(String gender) {
        assertThat((GenderType) ReflectionTestUtils.invokeMethod(service, "normalizeGender", gender))
                .isEqualTo(GenderType.W);
    }

    @Test
    void rejectsUnsupportedDadaeguGenderValue() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "normalizeGender", "unknown"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("성별 정보를 확인");
    }

    private UserDto.DadaeguLoginRequest encryptedLoginRequest(
            String did, String ci, String name, String birthdate, String phoneNumber, String gender
    ) throws Exception {
        ObjectNode claims = objectMapper.createObjectNode();
        claims.put("did", encrypt(did));
        claims.put("ci", encrypt(ci));
        claims.put("name", encrypt(name));
        claims.put("birthdate", encrypt(birthdate));
        claims.put("phoneNumber", encrypt(phoneNumber));
        claims.put("gender", encrypt(gender));
        ObjectNode callback = objectMapper.createObjectNode().set("response", claims);
        return UserDto.DadaeguLoginRequest.builder().encryptedData(callback).build();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return Base64.getEncoder().encodeToString(
                cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))
        );
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
