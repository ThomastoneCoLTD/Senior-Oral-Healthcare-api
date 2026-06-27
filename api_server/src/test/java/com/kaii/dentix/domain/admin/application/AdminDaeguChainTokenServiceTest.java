package com.kaii.dentix.domain.admin.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDaeguChainTokenServiceTest {

    private ExternalTokenClient externalTokenClient;
    private DaeguChainProperties properties;
    private AdminDaeguChainTokenService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        externalTokenClient = mock(ExternalTokenClient.class);
        properties = new DaeguChainProperties();
        properties.setTokenSymbol("MYT");
        service = new AdminDaeguChainTokenService(externalTokenClient, properties);
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
        when(externalTokenClient.getTokenList()).thenReturn(data);

        List<String> tokenNames = service.getTokenNames();

        assertThat(tokenNames).containsExactly("ESSENTIAL_VIDEO_1", "ESSENTIAL_VIDEO_2");
    }

    @Test
    void createTokenCallsExternalTokenServerWithConfiguredSymbol() throws Exception {
        JsonNode responseBody = objectMapper.readTree("""
                {
                  "contract address": "0x-token-contract"
                }
                """);
        when(externalTokenClient.createToken("ESSENTIAL_VIDEO_1", "MYT", 3L))
                .thenReturn(responseBody);

        var response = service.createToken(new AdminDaeguChainTokenDto.CreateRequest("ESSENTIAL_VIDEO_1", 3L));

        assertThat(response.getData().path("contract address").asText()).isEqualTo("0x-token-contract");
        verify(externalTokenClient).createToken("ESSENTIAL_VIDEO_1", "MYT", 3L);
    }

    @Test
    void createTokenRequiresTokenSymbolConfiguration() {
        properties.setTokenSymbol(null);

        assertThatThrownBy(() -> service.createToken(new AdminDaeguChainTokenDto.CreateRequest("ESSENTIAL_VIDEO_1", 1L)))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("daegu-chain.token-symbol is required");
    }
}
