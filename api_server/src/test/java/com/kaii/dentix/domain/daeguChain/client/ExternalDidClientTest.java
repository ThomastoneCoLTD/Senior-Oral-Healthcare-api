package com.kaii.dentix.domain.daeguChain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalDidClientTest {

    private MockRestServiceServer server;
    private ExternalDidClient client;

    @BeforeEach
    void setUp() {
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setDidServerBaseUrl("https://did.example.com/");
        properties.setDidCreatePath("/did/create");
        properties.setDidIssueVcPath("/did/issue-vc");
        properties.setDidVerifyVcPath("/did/verify-vc");

        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        client = new ExternalDidClient(properties, new RestTemplateBuilder(customizer));
        server = customizer.getServer();
    }

    @Test
    void createDidPostsConfiguredCreatePath() {
        server.expect(once(), requestTo("https://did.example.com/did/create"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"label\":\"soh-user-001\"")))
                .andRespond(withSuccess("""
                        {
                          "did": "did:key:z6MkUser",
                          "fingerprint": "z6MkUser"
                        }
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = client.createDid(Map.of("label", "soh-user-001"));

        assertThat(response.path("did").asText()).isEqualTo("did:key:z6MkUser");
        server.verify();
    }

    @Test
    void issueVcPostsConfiguredIssuePath() {
        server.expect(once(), requestTo("https://did.example.com/did/issue-vc"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"issuer\":\"did:key:z6MkUser\"")))
                .andExpect(content().string(containsString("\"subject\":\"did:key:z6MkUser\"")))
                .andRespond(withSuccess("""
                        {
                          "vc_jwt": "credential-jwt",
                          "exp": 1793404800
                        }
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = client.issueVc(Map.of(
                "issuer", "did:key:z6MkUser",
                "subject", "did:key:z6MkUser"
        ));

        assertThat(response.path("vc_jwt").asText()).isEqualTo("credential-jwt");
        server.verify();
    }

    @Test
    void verifyVcPostsConfiguredVerifyPath() {
        server.expect(once(), requestTo("https://did.example.com/did/verify-vc"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"vc_jwt\":\"credential-jwt\"")))
                .andRespond(withSuccess("""
                        {
                          "valid": true,
                          "payload": {
                            "iss": "did:key:z6MkUser"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = client.verifyVc(Map.of("vc_jwt", "credential-jwt"));

        assertThat(response.path("valid").asBoolean()).isTrue();
        server.verify();
    }
}
