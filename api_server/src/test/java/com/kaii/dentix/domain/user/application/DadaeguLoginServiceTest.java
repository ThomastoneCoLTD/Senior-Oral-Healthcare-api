package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.config.DadaeguLoginProperties;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DadaeguLoginServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DadaeguLoginProperties properties;
    private UserRepository userRepository;
    private UserLoginService userLoginService;
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
        userLoginService = mock(UserLoginService.class);
        service = new DadaeguLoginService(properties, userRepository, userLoginService, objectMapper);
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
                .userId(31L)
                .accessToken("access-token")
                .build();

        when(userRepository.findByDaeguDid("did:daegu:test-user")).thenReturn(Optional.empty());
        when(userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                "01012345678", "홍길동", "1950-01-02"
        )).thenReturn(Optional.of(user));
        when(userLoginService.completeAuthenticatedLogin(user)).thenReturn(expected);

        ObjectNode claims = objectMapper.createObjectNode();
        claims.put("did", encrypt("did:daegu:test-user"));
        claims.put("name", encrypt("홍길동"));
        claims.put("birthdate", encrypt("19500102"));
        claims.put("phoneNumber", encrypt("010-1234-5678"));
        claims.put("gender", encrypt("M"));
        claims.put("isForeigner", encrypt("N"));
        claims.put("ci", encrypt("test-ci"));
        ObjectNode callback = objectMapper.createObjectNode().set("response", claims);

        UserDto.LoginResponse response = service.login(
                UserDto.DadaeguLoginRequest.builder().encryptedData(callback).build()
        );

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userLoginService).completeAuthenticatedLogin(user);
    }

    @Test
    void loginIsRejectedUntilServerCredentialsAreConfigured() {
        properties.setRsaPrivateKey("");
        ObjectNode callback = objectMapper.createObjectNode().put("did", "anything");

        assertThatThrownBy(() -> service.login(
                UserDto.DadaeguLoginRequest.builder().encryptedData(callback).build()
        )).isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("연동 정보");
    }

    @Test
    void encryptedCallbackPayloadIsMaskedDuringRequestLogging() throws Exception {
        ObjectNode encrypted = objectMapper.createObjectNode().put("did", "secret-ciphertext");
        UserDto.DadaeguLoginRequest request = UserDto.DadaeguLoginRequest.builder()
                .encryptedData(encrypted)
                .build();

        assertThat(objectMapper.writeValueAsString(request))
                .isEqualTo("{\"encryptedData\":\"********\"}");
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return Base64.getEncoder().encodeToString(
                cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))
        );
    }
}
