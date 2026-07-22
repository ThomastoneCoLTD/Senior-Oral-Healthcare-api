package com.kaii.dentix.domain.daeguChain.client;

import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ExternalTokenClientTest {

    private DaeguChainProperties properties;
    private ExternalTokenClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new DaeguChainProperties();
        properties.setAppKey("app-token");
        properties.setTokenServerBaseUrl("https://token.example.com");
        properties.setTokenCreatePath("/token/create");
        properties.setTokenTransferPath("/token/transfer");
        properties.setTokenListPath("/token/token_list");

        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        client = new ExternalTokenClient(properties, new RestTemplateBuilder(customizer));
        server = customizer.getServer();
    }

    @Test
    void getTokenListPostsConfiguredAppToken() {
        server.expect(once(), requestTo("https://token.example.com/token/token_list"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"token\":\"app-token\"")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.getTokenList();

        server.verify();
    }

    @Test
    void createTokenPostsConfiguredAppToken() {
        server.expect(once(), requestTo("https://token.example.com/token/create"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"token\":\"app-token\"")))
                .andExpect(content().string(containsString("\"token_name\":\"ESSENTIAL_VIDEO_1\"")))
                .andExpect(content().string(containsString("\"token_symbol\":\"MYT\"")))
                .andExpect(content().string(containsString("\"supply\":100")))
                .andRespond(withSuccess("{\"res\":true}", MediaType.APPLICATION_JSON));

        client.createToken("ESSENTIAL_VIDEO_1", "MYT", 100L);

        server.verify();
    }

    @Test
    void transferTokenPostsConfiguredAppToken() {
        server.expect(once(), requestTo("https://token.example.com/token/transfer"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"token\":\"app-token\"")))
                .andExpect(content().string(containsString("\"token_name\":\"ESSENTIAL_VIDEO_1\"")))
                .andExpect(content().string(containsString("\"contract\":\"0x-token\"")))
                .andExpect(content().string(containsString("\"receiver\":\"0x-user\"")))
                .andExpect(content().string(containsString("\"amount\":1")))
                .andRespond(withSuccess("{\"res\":true}", MediaType.APPLICATION_JSON));

        client.transferToken("did:example:user", "ESSENTIAL_VIDEO_1", "0x-token", "0x-user", 1L);

        server.verify();
    }

    @Test
    void transferTokenRejectsCreatePathConfiguration() {
        properties.setTokenTransferPath("/token/create");

        assertThatThrownBy(() -> client.transferToken("did:example:user", "ESSENTIAL_VIDEO_1", "0x-token", "0x-user", 1L))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("token-transfer-path must not equal token-create-path");
    }

    @Test
    void transferTokenSurfacesTokenServerErrorMessage() {
        server.expect(once(), requestTo("https://token.example.com/token/transfer"))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"state\":\"ERROR\",\"msg\":\"user_DID not found\"}"));

        assertThatThrownBy(() -> client.transferToken("did:key:user", "ESSENTIAL_VIDEO_1", "0x-token", "0x-user", 1L))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("Token server API call failed: user_DID not found");
    }

    @Test
    void requestRequiresConfiguredAppToken() {
        properties.setAppKey(null);
        properties.setToken(null);

        assertThatThrownBy(() -> client.getTokenList())
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("daegu-chain.app-key or daegu-chain.token is required");
    }
}
