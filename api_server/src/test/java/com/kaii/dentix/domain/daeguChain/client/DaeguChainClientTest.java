package com.kaii.dentix.domain.daeguChain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class DaeguChainClientTest {

    private MockRestServiceServer server;
    private DaeguChainClient client;

    @BeforeEach
    void setUp() {
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setBaseUrl("https://www.daegu.go.kr/daeguchain");
        properties.setApiVersion("v2");
        properties.setChain("dchain");
        properties.setToken("app-token");

        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        client = new DaeguChainClient(properties, new RestTemplateBuilder(customizer));
        server = customizer.getServer();
    }

    @Test
    void getRpcNodePostsChainAndMapsResponse() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/com/rpc_node"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"chain\":\"dchain\"")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "rpc_node": "https://testnet.imfact.im"
                          },
                          "cid": "cid-rpc"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<DaeguChainDto.RpcNodeData> response =
                client.getRpcNode(DaeguChainDto.ChainRequest.builder()
                        .chain("dchain")
                        .build());

        assertThat(response.getState()).isEqualTo("OK");
        assertThat(response.getData().getRpcNode()).isEqualTo("https://testnet.imfact.im");
        assertThat(response.getCid()).isEqualTo("cid-rpc");
        server.verify();
    }

    @Test
    void createAccountPostsTokenAndMapsKeyPair() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/com/acc_create"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"token\":\"app-token\"")))
                .andExpect(content().string(containsString("\"chain\":\"dchain\"")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "key_pair": {
                              "privatekey": "private-key",
                              "publickey": "public-key",
                              "address": "0x123"
                            }
                          },
                          "cid": "cid-account"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> response =
                client.createAccount(DaeguChainDto.TokenChainRequest.builder()
                        .token("app-token")
                        .chain("dchain")
                        .build());

        assertThat(response.getData().getKeyPair().getPrivatekey()).isEqualTo("private-key");
        assertThat(response.getData().getKeyPair().getPublickey()).isEqualTo("public-key");
        assertThat(response.getData().getKeyPair().getAddress()).isEqualTo("0x123");
        server.verify();
    }

    @Test
    void getBlockByNumberPreservesBlockJson() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/com/block_by_num"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"block_num\":\"123\"")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "block": {
                              "Manifest": {
                                "hash": "Y4GQh5HyihGDzyPjwxKBdJjingm4nEHN2s6q3ajaVQb",
                                "height": 123
                              },
                              "operations": 1,
                              "proposer": "node0sas"
                            }
                          },
                          "cid": "cid-block"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<JsonNode> response =
                client.getBlockByNumber(DaeguChainDto.BlockByNumberApiRequest.builder()
                        .token("app-token")
                        .chain("dchain")
                        .blockNum("123")
                        .build());

        JsonNode block = response.getData().get("block");
        assertThat(block.get("Manifest").get("height").asInt()).isEqualTo(123);
        assertThat(block.get("operations").asInt()).isEqualTo(1);
        assertThat(block.get("proposer").asText()).isEqualTo("node0sas");
        server.verify();
    }

    @Test
    void createTokenPostsToken20Payload() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/token/create"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"token_name\":\"MYTOKEN\"")))
                .andExpect(content().string(containsString("\"owner_addr\":\"0x-owner\"")))
                .andExpect(content().string(containsString("\"decimals\":18")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "contract": {
                              "data": {
                                "address": "0x-token-contract"
                              }
                            }
                          },
                          "cid": "cid-token-create"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<JsonNode> response =
                client.createToken(DaeguChainDto.TokenCreateApiRequest.builder()
                        .token("app-token")
                        .chain("dchain")
                        .chainName("mitum testnet")
                        .ownerAddr("0x-owner")
                        .ownerPkey("owner-private-key")
                        .tokenName("MYTOKEN")
                        .tokenSymbol("MYT")
                        .decimals(18)
                        .supply(10_000_000_000L)
                        .mintable(true)
                        .lockable(true)
                        .build());

        assertThat(response.getData().get("contract").get("data").get("address").asText())
                .isEqualTo("0x-token-contract");
        server.verify();
    }

    @Test
    void uploadTokenPostsMultipartPayload() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/upload/upload_token"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("app-token")))
                .andExpect(content().string(containsString("0x-token-contract")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "file_name": "token.png",
                            "cont_addr": "0x-token-contract",
                            "uri": "https://baas.api.imfact.im/upload/token/0x-token-contract.png"
                          },
                          "cid": "cid-token-upload"
                        }
                        """, MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
                "tokenFile",
                "token.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        DaeguChainDto.ApiResponse<JsonNode> response =
                client.uploadToken("app-token", "0x-token-contract", file);

        assertThat(response.getData().get("file_name").asText()).isEqualTo("token.png");
        assertThat(response.getData().get("cont_addr").asText()).isEqualTo("0x-token-contract");
        server.verify();
    }

    @Test
    void postNftPostsNftPayload() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/nft/create"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"nft_name\":\"testNFT\"")))
                .andExpect(content().string(containsString("\"owner_addr\":\"0x-owner\"")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "contract": "0x-nft-contract"
                          },
                          "cid": "cid-nft-create"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<JsonNode> response = client.postNft(
                "/mitum/nft/create",
                Map.of(
                        "token", "app-token",
                        "chain", "dchain",
                        "owner_addr", "0x-owner",
                        "owner_pkey", "owner-private-key",
                        "nft_name", "testNFT",
                        "nft_uri", "https://example.com/nft.json",
                        "royalty", "3"
                )
        );

        assertThat(response.getData().get("contract").asText()).isEqualTo("0x-nft-contract");
        server.verify();
    }

    @Test
    void uploadNftPostsMultipartPayload() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/upload/upload_nft"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("app-token")))
                .andExpect(content().string(containsString("Test Nft")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "file_name": "nft.png",
                            "uri": "https://baas.api.imfact.im/upload/nft/nft.png"
                          },
                          "cid": "cid-nft-upload"
                        }
                        """, MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
                "nftFile",
                "nft.png",
                "image/png",
                new byte[] {4, 5, 6}
        );

        DaeguChainDto.ApiResponse<JsonNode> response =
                client.uploadNft("app-token", "Test Nft", file);

        assertThat(response.getData().get("file_name").asText()).isEqualTo("nft.png");
        server.verify();
    }

    @Test
    void postDidPostsDidPayload() {
        server.expect(once(), requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/did/issue"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"did\":\"did:mitum:minic:0x123\"")))
                .andExpect(content().string(containsString("\"template_id\":\"template-id\"")))
                .andExpect(content().string(containsString("\"subject\"")))
                .andExpect(content().string(containsString("\"key\":\"serial\"")))
                .andExpect(content().string(containsString("\"value\":\"12345\"")))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "jwt": "credential-jwt"
                          },
                          "cid": "cid-did-issue"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<JsonNode> response = client.postDid(
                "/mitum/did/issue",
                Map.of(
                        "token", "app-token",
                        "chain", "dchain",
                        "did", "did:mitum:minic:0x123",
                        "template_id", "template-id",
                        "subject", Map.of("key", "serial", "value", "12345"),
                        "validfrom", "2024-08-16",
                        "validuntil", "2024-08-31"
                )
        );

        assertThat(response.getData().get("jwt").asText()).isEqualTo("credential-jwt");
        server.verify();
    }

    @Test
    void apiBaseUrlCanPointDirectlyToExternalMitumApiRoot() {
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setApiBaseUrl("https://did-api.example.com/v2/mitum");

        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        DaeguChainClient externalClient = new DaeguChainClient(properties, new RestTemplateBuilder(customizer));
        MockRestServiceServer externalServer = customizer.getServer();

        externalServer.expect(once(), requestTo("https://did-api.example.com/v2/mitum/did/create_account"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {
                          "state": "OK",
                          "rcode": {},
                          "msg": "",
                          "data": {
                            "did": "did:mitum:minic:0x123"
                          },
                          "cid": "cid-did-create"
                        }
                        """, MediaType.APPLICATION_JSON));

        DaeguChainDto.ApiResponse<JsonNode> response = externalClient.postDid(
                "/mitum/did/create_account",
                Map.of("token", "external-token", "chain", "dchain")
        );

        assertThat(response.getData().get("did").asText()).isEqualTo("did:mitum:minic:0x123");
        externalServer.verify();
    }
}
