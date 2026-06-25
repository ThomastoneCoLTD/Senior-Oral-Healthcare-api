package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DaeguChainToken20ServiceTest {

    @Mock
    private DaeguChainClient daeguChainClient;

    private DaeguChainProperties properties;
    private DaeguChainToken20Service service;

    @BeforeEach
    void setUp() {
        properties = new DaeguChainProperties();
        properties.setChain("dchain");
        properties.setToken("configured-token");
        service = new DaeguChainToken20Service(daeguChainClient, properties);
    }

    @Test
    void getTokenListUsesConfiguredTokenAndChainWhenRequestOmitsThem() {
        service.getTokenList(new DaeguChainDto.TokenListRequest(null, null));

        ArgumentCaptor<DaeguChainDto.TokenListApiRequest> captor =
                ArgumentCaptor.forClass(DaeguChainDto.TokenListApiRequest.class);
        verify(daeguChainClient).getTokenList(captor.capture());

        assertThat(captor.getValue().getToken()).isEqualTo("configured-token");
        assertThat(captor.getValue().getChain()).isEqualTo("dchain");
    }

    @Test
    void getTokenListUsesConfiguredAppKeyBeforeRequestToken() {
        properties.setAppKey("configured-app-key");

        service.getTokenList(new DaeguChainDto.TokenListRequest("request-token", null));

        ArgumentCaptor<DaeguChainDto.TokenListApiRequest> captor =
                ArgumentCaptor.forClass(DaeguChainDto.TokenListApiRequest.class);
        verify(daeguChainClient).getTokenList(captor.capture());

        assertThat(captor.getValue().getToken()).isEqualTo("configured-app-key");
    }

    @Test
    void uploadTokenRequiresContractAddress() {
        assertThatThrownBy(() -> service.uploadToken("token", "", null))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("contAddr is required");
    }
}
