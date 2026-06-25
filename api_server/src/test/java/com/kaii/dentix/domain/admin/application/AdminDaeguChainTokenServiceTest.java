package com.kaii.dentix.domain.admin.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainToken20Service;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDaeguChainTokenServiceTest {

    private DaeguChainToken20Service daeguChainToken20Service;
    private DaeguChainProperties properties;
    private AdminDaeguChainTokenService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        daeguChainToken20Service = mock(DaeguChainToken20Service.class);
        properties = new DaeguChainProperties();
        properties.setTokenOwnerAddress("0x-owner");
        properties.setTokenOwnerPrivateKey("owner-private-key");
        properties.setTokenSymbol("MYT");
        properties.setTokenDecimals(18);
        properties.setTokenMintable(true);
        properties.setTokenLockable(true);
        service = new AdminDaeguChainTokenService(daeguChainToken20Service, properties);
        objectMapper = new ObjectMapper();
    }

    @Test
    void getTokenNamesReturnsOnlyTokenDataNames() throws Exception {
        JsonNode data = objectMapper.readTree("""
                [
                  {
                    "contract": "0x-token",
                    "data": {
                      "name": "ESSENTIAL_VIDEO_1",
                      "symbol": "MYT"
                    }
                  },
                  {
                    "contract": "0x-token2",
                    "data": {
                      "name": "ESSENTIAL_VIDEO_2",
                      "symbol": "MYT"
                    }
                  }
                ]
                """);
        when(daeguChainToken20Service.getTokenList(any(DaeguChainDto.TokenListRequest.class)))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", Map.of(), "", data, "cid"));

        List<String> tokenNames = service.getTokenNames();

        assertThat(tokenNames).containsExactly("ESSENTIAL_VIDEO_1", "ESSENTIAL_VIDEO_2");
    }

    @Test
    void createTokenBuildsConfiguredOwnerTokenRequest() {
        service.createToken(new AdminDaeguChainTokenDto.CreateRequest("ESSENTIAL_VIDEO_1", 3L));

        ArgumentCaptor<DaeguChainDto.TokenCreateRequest> captor =
                ArgumentCaptor.forClass(DaeguChainDto.TokenCreateRequest.class);
        verify(daeguChainToken20Service).createToken(captor.capture());

        assertThat(captor.getValue().getToken()).isNull();
        assertThat(captor.getValue().getOwnerAddr()).isEqualTo("0x-owner");
        assertThat(captor.getValue().getOwnerPkey()).isEqualTo("owner-private-key");
        assertThat(captor.getValue().getTokenName()).isEqualTo("ESSENTIAL_VIDEO_1");
        assertThat(captor.getValue().getTokenSymbol()).isEqualTo("MYT");
        assertThat(captor.getValue().getDecimals()).isEqualTo(18);
        assertThat(captor.getValue().getSupply()).isEqualTo(3L);
        assertThat(captor.getValue().getMintable()).isTrue();
        assertThat(captor.getValue().getLockable()).isTrue();
    }

    @Test
    void createTokenRequiresOwnerPrivateKeyConfiguration() {
        properties.setTokenOwnerPrivateKey(null);

        assertThatThrownBy(() -> service.createToken(new AdminDaeguChainTokenDto.CreateRequest("ESSENTIAL_VIDEO_1", 1L)))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("daegu-chain.token-owner-private-key is required");
    }
}
