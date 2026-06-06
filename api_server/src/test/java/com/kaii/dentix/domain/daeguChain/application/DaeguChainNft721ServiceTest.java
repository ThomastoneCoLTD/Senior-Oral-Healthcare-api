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
class DaeguChainNft721ServiceTest {

    @Mock
    private DaeguChainClient daeguChainClient;

    private DaeguChainNft721Service service;

    @BeforeEach
    void setUp() {
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setChain("dchain");
        properties.setToken("configured-token");
        service = new DaeguChainNft721Service(daeguChainClient, properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUsesConfiguredTokenAndChainWhenRequestOmitsThem() {
        service.list(null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postNft(eq("/mitum/nft/nfts"), captor.capture());

        assertThat(captor.getValue().get("token")).isEqualTo("configured-token");
        assertThat(captor.getValue().get("chain")).isEqualTo("dchain");
    }

    @Test
    void createRequiresOwnerAddress() {
        assertThatThrownBy(() -> service.create(Map.of(
                "owner_pkey", "owner-private-key",
                "nft_name", "testNFT",
                "nft_uri", "https://example.com/nft.json",
                "royalty", "3"
        )))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("owner_addr is required");
    }
}
