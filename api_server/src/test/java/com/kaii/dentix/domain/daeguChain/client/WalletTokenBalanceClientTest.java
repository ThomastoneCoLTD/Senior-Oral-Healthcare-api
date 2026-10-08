package com.kaii.dentix.domain.daeguChain.client;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class WalletTokenBalanceClientTest {
    @Test void queriesDirectConfiguredChainUsingServerTokenAndStoredWallet() {
        var server = new AtomicReference<MockRestServiceServer>();
        var builder = new RestTemplateBuilder(template -> {
            var mock = MockRestServiceServer.bindTo(template).build();
            mock.expect(requestTo("https://www.daegu.go.kr/daeguchain/v2/mitum/token/balance"))
                    .andExpect(method(POST))
                    .andExpect(content().json("{\"token\":\"test-token\",\"chain\":\"dchain\",\"cont_addr\":\"test-contract\",\"addr\":\"test-wallet\"}"))
                    .andRespond(withSuccess("{\"state\":\"OK\",\"data\":{\"balance\":\"9007199254740993.25\"}}", MediaType.APPLICATION_JSON));
            server.set(mock);
        });
        var client = new DaeguChainClient(new DaeguChainProperties(), builder);
        var response = client.getWalletTokenBalance(DaeguChainDto.TokenBalanceApiRequest.builder()
                .token("test-token").chain("dchain").contAddr("test-contract").addr("test-wallet").build());
        assertThat(response.getData().path("balance").asText()).isEqualTo("9007199254740993.25");
        server.get().verify();
    }
}
