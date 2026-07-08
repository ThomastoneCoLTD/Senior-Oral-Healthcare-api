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
                  },
                  {
                    "contract": "0x-other",
                    "data": {
                      "name": "UNRELATED_TOKEN",
                      "symbol": "MYT"
                    }
                  }
                ]
                """);
        when(externalTokenClient.getTokenList()).thenReturn(data);

        List<String> tokenNames = service.getTokenNames();

        assertThat(tokenNames).containsExactly(
                "ESSENTIAL_VIDEO_1",
                "ESSENTIAL_VIDEO_2",
                "ESSENTIAL_VIDEO_3",
                "ESSENTIAL_VIDEO_4",
                "ESSENTIAL_VIDEO_5",
                "OPTIONAL_VIDEO_1",
                "OPTIONAL_VIDEO_2",
                "OPTIONAL_VIDEO_3",
                "OPTIONAL_VIDEO_4",
                "OPTIONAL_VIDEO_5",
                "OPTIONAL_VIDEO_6",
                "OPTIONAL_VIDEO_7"
        );
    }

    @Test
    void getTokenOptionsReturnsRewardTokensWithIssueMetadata() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "contract": "0x-essential-5",
                      "data": {
                        "name": "ESSENTIAL_VIDEO_5",
                        "symbol": "MYT",
                        "supply": 100,
                        "decimals": 9,
                        "owner": "0x-owner"
                      },
                      "tx": {
                        "hash": "tx-hash",
                        "fact_hash": "fact-hash"
                      },
                      "issued": "2026-07-04T02:56:39.524Z"
                    },
                    {
                      "contract": "0x-optional-1",
                      "data": {
                        "name": "OPTIONAL_VIDEO_1",
                        "symbol": "MYT",
                        "supply": 100
                      }
                    }
                  ]
                }
                """);
        when(externalTokenClient.getTokenList()).thenReturn(data);

        List<AdminDaeguChainTokenDto.TokenOption> tokenOptions = service.getTokenOptions();

        assertThat(tokenOptions).extracting(AdminDaeguChainTokenDto.TokenOption::getTokenName)
                .containsExactly(
                        "ESSENTIAL_VIDEO_1",
                        "ESSENTIAL_VIDEO_2",
                        "ESSENTIAL_VIDEO_3",
                        "ESSENTIAL_VIDEO_4",
                        "ESSENTIAL_VIDEO_5",
                        "OPTIONAL_VIDEO_1",
                        "OPTIONAL_VIDEO_2",
                        "OPTIONAL_VIDEO_3",
                        "OPTIONAL_VIDEO_4",
                        "OPTIONAL_VIDEO_5",
                        "OPTIONAL_VIDEO_6",
                        "OPTIONAL_VIDEO_7"
                );
        AdminDaeguChainTokenDto.TokenOption essential5 = tokenOptions.get(4);
        assertThat(essential5.getContractAddress()).isEqualTo("0x-essential-5");
        assertThat(essential5.getDecimals()).isEqualTo(9);
        assertThat(essential5.getOwner()).isEqualTo("0x-owner");
        assertThat(essential5.getTxHash()).isEqualTo("tx-hash");
        assertThat(essential5.getFactHash()).isEqualTo("fact-hash");
        assertThat(essential5.getIssued()).isEqualTo("2026-07-04T02:56:39.524Z");
    }

    @Test
    void getTokenOptionsFallsBackToRewardTokenNamesWhenExternalTokenServerFails() {
        when(externalTokenClient.getTokenList()).thenThrow(new BadRequestApiException("token server error"));

        List<AdminDaeguChainTokenDto.TokenOption> tokenOptions = service.getTokenOptions();

        assertThat(tokenOptions).hasSize(12);
        assertThat(tokenOptions.get(0).getTokenName()).isEqualTo("ESSENTIAL_VIDEO_1");
        assertThat(tokenOptions.get(0).getContractAddress()).isEqualTo("ESSENTIAL_VIDEO_1");
        assertThat(tokenOptions.get(11).getTokenName()).isEqualTo("OPTIONAL_VIDEO_7");
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

    @Test
    void createTokenRejectsUnsupportedTokenName() {
        assertThatThrownBy(() -> service.createToken(new AdminDaeguChainTokenDto.CreateRequest("UNRELATED_TOKEN", 1L)))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("unsupported reward token name");
    }
}
