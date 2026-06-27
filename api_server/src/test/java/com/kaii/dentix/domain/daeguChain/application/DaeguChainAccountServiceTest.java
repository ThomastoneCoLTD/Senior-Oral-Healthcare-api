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
class DaeguChainAccountServiceTest {

    @Mock
    private DaeguChainClient daeguChainClient;

    private DaeguChainProperties properties;
    private DaeguChainAccountService service;

    @BeforeEach
    void setUp() {
        properties = new DaeguChainProperties();
        properties.setChain("dchain");
        properties.setToken("configured-token");
        service = new DaeguChainAccountService(daeguChainClient, properties);
    }

    @Test
    void createAccountUsesConfiguredTokenAndChainWhenRequestOmitsThem() {
        service.createAccount(new DaeguChainDto.AccountCreateRequest(null, null));

        ArgumentCaptor<DaeguChainDto.TokenChainRequest> captor =
                ArgumentCaptor.forClass(DaeguChainDto.TokenChainRequest.class);
        verify(daeguChainClient).createAccount(captor.capture());

        assertThat(captor.getValue().getToken()).isEqualTo("configured-token");
        assertThat(captor.getValue().getChain()).isEqualTo("dchain");
    }

    @Test
    void createAccountRequiresTokenWhenRequestAndConfigurationAreEmpty() {
        properties.setToken(null);

        assertThatThrownBy(() -> service.createAccount(new DaeguChainDto.AccountCreateRequest(null, null)))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("token is required");
    }
}
