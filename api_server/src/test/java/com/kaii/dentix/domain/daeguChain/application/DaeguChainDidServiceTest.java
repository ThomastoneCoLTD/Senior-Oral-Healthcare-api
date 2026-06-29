package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.client.ExternalDidClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguCredentialStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DaeguChainDidServiceTest {

    @Mock
    private DaeguChainClient daeguChainClient;

    @Mock
    private ExternalDidClient externalDidClient;

    @Mock
    private UserRepository userRepository;

    private DaeguChainProperties properties;
    private ObjectMapper objectMapper;
    private DaeguChainDidService service;

    @BeforeEach
    void setUp() {
        properties = new DaeguChainProperties();
        objectMapper = new ObjectMapper();
        properties.setChain("dchain");
        properties.setToken("configured-token");
        service = new DaeguChainDidService(daeguChainClient, externalDidClient, userRepository, properties, objectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void projectListUsesConfiguredTokenAndChainWhenRequestOmitsThem() {
        service.projectList(null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postDid(eq("/mitum/did/projects"), captor.capture());

        assertThat(captor.getValue().get("token")).isEqualTo("configured-token");
        assertThat(captor.getValue().get("chain")).isEqualTo("dchain");
    }

    @Test
    void issueCredentialRequiresSubject() {
        assertThatThrownBy(() -> service.issueCredential(Map.of(
                "did", "did:mitum:minic:0x123",
                "template_id", "template-id",
                "validfrom", "2024-08-16",
                "validuntil", "2024-08-31"
        )))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("subject is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void issueLoginUserCredentialUsesConfiguredTemplateAndStoresJwt() throws Exception {
        properties.setLoginUserCredentialValidFrom("2026-06-01");
        properties.setLoginUserCredentialValidUntil("2026-12-01");
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid("did:mitum:minic:0xabc")
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(daeguChainClient.postDid(eq("/mitum/did/issue"), any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        objectMapper.readTree("""
                                {
                                  "issue": {
                                    "data": {
                                      "jwt": "credential-jwt"
                                    }
                                  }
                                }
                                """),
                        "cid"
                ));

        service.issueLoginUserCredential(7L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postDid(eq("/mitum/did/issue"), captor.capture());

        assertThat(captor.getValue().get("token")).isEqualTo("configured-token");
        assertThat(captor.getValue().get("chain")).isEqualTo("dchain");
        assertThat(captor.getValue().get("did")).isEqualTo("did:mitum:minic:0xabc");
        assertThat(captor.getValue().get("template_id")).isEqualTo("VLVSWVRSOPZJMPINTBNA");
        assertThat(captor.getValue().get("validfrom")).isEqualTo("2026-06-01");
        assertThat(captor.getValue().get("validuntil")).isEqualTo("2026-12-01");

        Map<String, Object> subject = (Map<String, Object>) captor.getValue().get("subject");
        assertThat(subject.get("key")).isEqualTo("id");
        assertThat(subject.get("value")).isEqualTo("soh-user-001");
        assertThat(user.getDaeguCredentialJwt()).isEqualTo("credential-jwt");
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.ISSUED);
        assertThat(user.getDaeguCredentialValidFrom()).hasToString("2026-06-01");
        assertThat(user.getDaeguCredentialValidUntil()).hasToString("2026-12-01");
    }

    @Test
    void issueLoginUserCredentialRequiresUserDid() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder()
                .userId(7L)
                .build()));

        assertThatThrownBy(() -> service.issueLoginUserCredential(7L))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("user daeguDid is required");
    }

    @Test
    void issueLoginUserCredentialRequiresUserLoginIdentifier() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(User.builder()
                .userId(7L)
                .daeguDid("did:mitum:minic:0xabc")
                .build()));

        assertThatThrownBy(() -> service.issueLoginUserCredential(7L))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("userLoginIdentifier is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void verifyLoginUserCredentialAcceptsCredentialAudWithTemplateId() throws Exception {
        String did = "did:mitum:minic:0xabc";
        String jwt = jwtWithAud(did + ":VLVSWVRSOPZJMPINTBNA");
        User user = User.builder()
                .userId(7L)
                .daeguDid(did)
                .daeguCredentialJwt(jwt)
                .build();
        when(daeguChainClient.postDid(eq("/mitum/did/verification"), any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        objectMapper.readTree("""
                                {
                                  "verify": true
                                }
                                """),
                        "cid"
                ));

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postDid(eq("/mitum/did/verification"), captor.capture());
        assertThat(captor.getValue().get("token")).isEqualTo("configured-token");
        assertThat(captor.getValue().get("chain")).isEqualTo("dchain");
        assertThat(captor.getValue().get("template_id")).isEqualTo("VLVSWVRSOPZJMPINTBNA");
        assertThat(captor.getValue().get("jwt")).isEqualTo(jwt);
    }

    @Test
    void verifyLoginUserCredentialAcceptsStoredCredentialSubject() {
        String did = "did:mitum:minic:0xabc";
        String jwt = jwtWithClaims("""
                {
                  "aud": "did:mitum:minic:0xabc:VLVSWVRSOPZJMPINTBNA",
                  "val": "id|soh-user-001"
                }
                """);
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid(did)
                .daeguCredentialJwt(jwt)
                .build();

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void verifyLoginUserCredentialAcceptsStoredCredentialSubjectObject() {
        String did = "did:mitum:minic:0xabc";
        String jwt = jwtWithClaims("""
                {
                  "aud": "did:mitum:minic:0xabc:VLVSWVRSOPZJMPINTBNA",
                  "val": {
                    "key": "id",
                    "value": "soh-user-001"
                  }
                }
                """);
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid(did)
                .daeguCredentialJwt(jwt)
                .build();

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void verifyLoginUserCredentialAcceptsStoredCredentialSubjectIdObject() {
        String did = "did:mitum:minic:0xabc";
        String jwt = jwtWithClaims("""
                {
                  "aud": "did:mitum:minic:0xabc:VLVSWVRSOPZJMPINTBNA",
                  "val": {
                    "id": "soh-user-001"
                  }
                }
                """);
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid(did)
                .daeguCredentialJwt(jwt)
                .build();

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void verifyLoginUserCredentialAcceptsStoredCredentialSubjectJsonString() {
        String did = "did:mitum:minic:0xabc";
        String jwt = jwtWithClaims("""
                {
                  "aud": "did:mitum:minic:0xabc:VLVSWVRSOPZJMPINTBNA",
                  "val": "{\\"key\\":\\"id\\",\\"value\\":\\"soh-user-001\\"}"
                }
                """);
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid(did)
                .daeguCredentialJwt(jwt)
                .build();

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void verifyLoginUserCredentialAcceptsAudWithSameWalletAddress() {
        String jwt = jwtWithClaims("""
                {
                  "aud": "did:mitum:0xabc:VLVSWVRSOPZJMPINTBNA",
                  "val": "id|soh-user-001"
                }
                """);
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid("did:mitum:minic:0xabc")
                .daeguCredentialJwt(jwt)
                .build();

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void verifyLoginUserCredentialRejectsStoredCredentialSubjectForDifferentUser() {
        String jwt = jwtWithClaims("""
                {
                  "aud": "did:mitum:minic:0xabc:VLVSWVRSOPZJMPINTBNA",
                  "val": "id|another-user"
                }
                """);
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid("did:mitum:minic:0xabc")
                .daeguCredentialJwt(jwt)
                .build();

        assertThat(service.verifyLoginUserCredential(user)).isFalse();
    }

    @Test
    void verifyLoginUserCredentialRejectsDifferentAud() throws Exception {
        User user = User.builder()
                .userId(7L)
                .daeguDid("did:mitum:minic:0xabc")
                .daeguCredentialJwt(jwtWithAud("did:mitum:minic:0xdef:VLVSWVRSOPZJMPINTBNA"))
                .build();
        when(daeguChainClient.postDid(eq("/mitum/did/verification"), any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        objectMapper.readTree("""
                                {
                                  "verify": true
                                }
                                """),
                        "cid"
                ));

        assertThat(service.verifyLoginUserCredential(user)).isFalse();
    }

    @Test
    void createAccountUsesExternalDidServerAndExtractsWalletAddress() throws Exception {
        when(externalDidClient.createDid(any()))
                .thenReturn(new ObjectMapper().readTree("""
                        {
                          "res": true,
                          "DID": "did:mitum:minic:0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca"
                        }
                        """));

        var response = service.createAccount(null);

        assertThat(response.getState()).isEqualTo("OK");
        assertThat(response.getData().path("did").asText())
                .isEqualTo("did:mitum:minic:0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca");
        assertThat(response.getData().path("address").asText())
                .isEqualTo("0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca");
        verify(externalDidClient).createDid(Map.of());
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registProjectPostsSohIssuerPayload() {
        service.registProject(Map.ofEntries(
                Map.entry("chain", "mitumt"),
                Map.entry("operation", "0"),
                Map.entry("project_id", "soh"),
                Map.entry("project_name", "Senior-Oral-Healthcare"),
                Map.entry("issuer_name", "thomastone"),
                Map.entry("company_name", "thomastone"),
                Map.entry("service_name", "Senior oral healthcare"),
                Map.entry("display_name", "TEST DID DISPLAY"),
                Map.entry("service_url", "http://localhost:3000/daeguchain/baas"),
                Map.entry("icon_url", "http://localhost:3000/daeguchain/icon.jpg")
        ));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postDid(eq("/mitum/did/regist_project"), captor.capture());

        assertThat(captor.getValue().get("token")).isEqualTo("configured-token");
        assertThat(captor.getValue().get("chain")).isEqualTo("mitumt");
        assertThat(captor.getValue().get("operation")).isEqualTo("0");
        assertThat(captor.getValue().get("project_id")).isEqualTo("soh");
        assertThat(captor.getValue().get("project_name")).isEqualTo("Senior-Oral-Healthcare");
        assertThat(captor.getValue().get("issuer_name")).isEqualTo("thomastone");
        assertThat(captor.getValue().get("company_name")).isEqualTo("thomastone");
        assertThat(captor.getValue().get("service_name")).isEqualTo("Senior oral healthcare");
        assertThat(captor.getValue().get("display_name")).isEqualTo("TEST DID DISPLAY");
        assertThat(captor.getValue().get("service_url")).isEqualTo("http://localhost:3000/daeguchain/baas");
        assertThat(captor.getValue().get("icon_url")).isEqualTo("http://localhost:3000/daeguchain/icon.jpg");
    }

    @Test
    void registProjectRequiresIssuerName() {
        assertThatThrownBy(() -> service.registProject(Map.ofEntries(
                Map.entry("operation", "0"),
                Map.entry("project_id", "soh"),
                Map.entry("project_name", "Senior-Oral-Healthcare"),
                Map.entry("company_name", "thomastone"),
                Map.entry("service_name", "Senior oral healthcare"),
                Map.entry("display_name", "TEST DID DISPLAY"),
                Map.entry("service_url", "http://localhost:3000/daeguchain/baas"),
                Map.entry("icon_url", "http://localhost:3000/daeguchain/icon.jpg")
        )))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("issuer_name is required");
    }

    private String jwtWithAud(String aud) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"aud\":\"" + aud + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    private String jwtWithClaims(String claims) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claims.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }
}
