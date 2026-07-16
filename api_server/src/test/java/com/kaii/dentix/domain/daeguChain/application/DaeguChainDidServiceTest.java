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
        when(externalDidClient.issueVc(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "vc_jwt": "credential-jwt",
                          "exp": 1793404800
                        }
                        """));

        service.issueLoginUserCredential(7L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(externalDidClient).issueVc(captor.capture());

        assertThat(captor.getValue().get("issuer")).isEqualTo("did:mitum:minic:0xabc");
        assertThat(captor.getValue().get("subject")).isEqualTo("did:mitum:minic:0xabc");
        assertThat(captor.getValue().get("aud")).isEqualTo("VLVSWVRSOPZJMPINTBNA");
        assertThat(captor.getValue().get("ttl")).isEqualTo(15_811_200L);

        Map<String, Object> claims = (Map<String, Object>) captor.getValue().get("claims");
        assertThat(claims.get("id")).isEqualTo("soh-user-001");
        assertThat(claims.get("userIdentifier")).isEqualTo("soh-user-001");
        assertThat(user.getDaeguCredentialJwt()).isEqualTo("credential-jwt");
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.ISSUED);
        assertThat(user.getDaeguCredentialValidFrom()).hasToString("2026-06-01");
        assertThat(user.getDaeguCredentialValidUntil()).hasToString("2026-12-01");
        verifyNoMoreInteractions(daeguChainClient);
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
    void verifyLoginUserCredentialCallsExternalVerifyVcAndAcceptsValidPayload() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid("did:key:z6MkUser")
                .daeguCredentialJwt("credential-jwt")
                .build();
        when(externalDidClient.verifyVc(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "valid": true,
                          "payload": {
                            "iss": "did:key:z6MkUser",
                            "sub": "did:key:z6MkUser",
                            "vc": {
                              "credentialSubject": {
                                "id": "soh-user-001",
                                "userIdentifier": "soh-user-001"
                              }
                            }
                          }
                        }
                        """));

        boolean verified = service.verifyLoginUserCredential(user);

        assertThat(verified).isTrue();
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(externalDidClient).verifyVc(captor.capture());
        assertThat(captor.getValue().get("vc_jwt")).isEqualTo("credential-jwt");
        assertThat(captor.getValue().get("aud")).isEqualTo("VLVSWVRSOPZJMPINTBNA");
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void verifyLoginUserCredentialRejectsExternalInvalidResponse() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid("did:key:z6MkUser")
                .daeguCredentialJwt("credential-jwt")
                .build();
        when(externalDidClient.verifyVc(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "valid": false,
                          "reason": "expired"
                        }
                        """));

        assertThat(service.verifyLoginUserCredential(user)).isFalse();
    }

    @Test
    void verifyLoginUserCredentialRejectsCredentialForDifferentUser() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .daeguDid("did:key:z6MkUser")
                .daeguCredentialJwt("credential-jwt")
                .build();
        when(externalDidClient.verifyVc(any()))
                .thenReturn(objectMapper.readTree("""
                        {
                          "valid": true,
                          "payload": {
                            "iss": "did:key:z6MkUser",
                            "sub": "did:key:z6MkUser",
                            "vc": {
                              "credentialSubject": {
                                "id": "another-user"
                              }
                            }
                          }
                        }
                        """));

        assertThat(service.verifyLoginUserCredential(user)).isFalse();
    }

    @Test
    void createAccountUsesExternalDidServerIssuedWalletAddress() throws Exception {
        when(externalDidClient.createDid(any()))
                .thenReturn(new ObjectMapper().readTree("""
                        {
                          "res": true,
                          "DID": "did:key:z6MkSelfGenerated",
                          "wallet_address": "0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca"
                        }
                        """));

        var response = service.createAccount(null);

        assertThat(response.getState()).isEqualTo("OK");
        assertThat(response.getData().path("did").asText())
                .isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(response.getData().path("address").asText())
                .isEqualTo("0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca");
        assertThat(response.getData().path("walletAddress").asText())
                .isEqualTo("0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca");
        verify(externalDidClient).createDid(Map.of());
        verifyNoMoreInteractions(daeguChainClient);
    }

    @Test
    void createAccountSupportsDidKeyCreateResponse() throws Exception {
        when(externalDidClient.createDid(any()))
                .thenReturn(new ObjectMapper().readTree("""
                        {
                          "did": "did:key:z6MksUrfsCNDrFRy3FsBL1imBc4WhS3JzPEk3uPELa8aWUpe",
                          "fingerprint": "z6MksUrfsCNDrFRy3FsBL1imBc4WhS3JzPEk3uPELa8aWUpe",
                          "label": "test-user",
                          "stored": {
                            "didDocumentPath": "data/dids/z6MksUrfsCNDrFRy3FsBL1imBc4WhS3JzPEk3uPELa8aWUpe.did.json"
                          }
                        }
                        """));

        var response = service.createAccount(Map.of(
                "label", "test-user"
        ));

        assertThat(response.getState()).isEqualTo("OK");
        assertThat(response.getData().path("did").asText())
                .isEqualTo("did:key:z6MksUrfsCNDrFRy3FsBL1imBc4WhS3JzPEk3uPELa8aWUpe");
        assertThat(response.getData().has("address")).isFalse();
        assertThat(response.getData().path("external_response").path("stored").path("didDocumentPath").asText())
                .contains("z6MksUrfsCNDrFRy3FsBL1imBc4WhS3JzPEk3uPELa8aWUpe.did.json");
        verify(externalDidClient).createDid(Map.of(
                "label", "test-user"
        ));
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
}
