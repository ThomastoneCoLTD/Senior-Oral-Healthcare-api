package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DaeguChainDidServiceTest {

    @Mock
    private DaeguChainClient daeguChainClient;

    private DaeguChainDidService service;

    @BeforeEach
    void setUp() {
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setChain("dchain");
        properties.setToken("configured-token");
        service = new DaeguChainDidService(daeguChainClient, properties);
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
}
