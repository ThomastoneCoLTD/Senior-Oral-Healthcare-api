package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserWalletTokenBalanceServiceTest {
    private final UserRewardWalletRepository wallets = mock(UserRewardWalletRepository.class);
    private final JwtTokenUtil jwt = mock(JwtTokenUtil.class);
    private final DaeguChainClient chain = mock(DaeguChainClient.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final DaeguChainProperties properties = new DaeguChainProperties();
    private UserWalletTokenBalanceService service;
    private UserRewardWallet wallet;

    @BeforeEach void setup() {
        properties.setToken("test-only-token");
        properties.getRewardTokenContracts().put("OPTIONAL_VIDEO_1", "contract-one");
        properties.getRewardTokenContracts().put("ESSENTIAL_VIDEO_1", "contract-two");
        properties.getRewardTokenContracts().put("UNRELATED", "ignored");
        when(jwt.getAccessToken(request)).thenReturn("test-access");
        when(jwt.getUserId("test-access", TokenType.AccessToken)).thenReturn(7L);
        wallet = UserRewardWallet.builder().userId(7L).walletAddress("my-wallet").pointBalance(91).build();
        when(wallets.findByUserId(7L)).thenReturn(Optional.of(wallet));
        service = new UserWalletTokenBalanceService(wallets, jwt, properties, chain);
    }
    @AfterEach void close() { service.close(); }

    @Test void readsOnlyOwnWalletAndKeepsRecordedPointsUnchanged() throws Exception {
        when(chain.getWalletTokenBalance(any())).thenAnswer(call -> {
            DaeguChainDto.TokenBalanceApiRequest payload = call.getArgument(0);
            assertThat(payload.getAddr()).isEqualTo("my-wallet");
            assertThat(payload.getToken()).isEqualTo("test-only-token");
            return response("OK", "\"9007199254740993.25\"");
        });
        var result = service.getBalances(request);
        assertThat(result.status()).isEqualTo("CONNECTED");
        assertThat(result.tokens()).hasSize(12);
        assertThat(result.tokens()).filteredOn(t -> t.status().equals("AVAILABLE"))
                .extracting(t -> t.balance()).containsExactly("9007199254740993.25", "9007199254740993.25");
        assertThat(wallet.getPointBalance()).isEqualTo(91);
        verify(chain, times(2)).getWalletTokenBalance(any());
        verify(wallets, never()).save(any());
        verify(wallets, never()).findByUserIdForUpdate(any());
    }

    @Test void absentWalletDoesNotProvisionOrQueryChain() {
        when(wallets.findByUserId(7L)).thenReturn(Optional.empty());
        assertThat(service.getBalances(request).status()).isEqualTo("NOT_CONNECTED");
        verifyNoInteractions(chain);
        verify(wallets, never()).save(any());
    }

    @Test void missingAuthenticationIsRejectedBeforeDatabaseLookup() {
        when(jwt.getAccessToken(request)).thenReturn(null);
        assertThatThrownBy(() -> service.getBalances(request)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(wallets, chain);
    }

    @Test void partialFailureDoesNotEraseSuccessfulBalanceOrExposeUpstreamError() throws Exception {
        when(chain.getWalletTokenBalance(any())).thenAnswer(call -> {
            DaeguChainDto.TokenBalanceApiRequest payload = call.getArgument(0);
            if (payload.getContAddr().equals("contract-two")) throw new IllegalStateException("secret-upstream");
            return response("OK", "\"0\"");
        });
        var result = service.getBalances(request);
        assertThat(result.tokens()).filteredOn(t -> t.tokenName().equals("OPTIONAL_VIDEO_1"))
                .extracting(t -> t.balance()).containsExactly("0");
        assertThat(result.tokens()).filteredOn(t -> t.tokenName().equals("ESSENTIAL_VIDEO_1"))
                .extracting(t -> t.balance()).containsExactly((String) null);
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(result)).doesNotContain("secret-upstream", "test-only-token");
    }

    @ParameterizedTest @ValueSource(strings={"null", "\"\"", "\"-1\"", "\"NaN\"", "{}", "true", "\"1e3\"", "0.100000000000000005"})
    void malformedBalanceIsUnavailableRatherThanZero(String value) throws Exception {
        when(chain.getWalletTokenBalance(any())).thenReturn(response("OK", value));
        assertThat(service.getBalances(request).tokens()).allMatch(t -> t.balance() == null && t.status().equals("UNAVAILABLE"));
    }

    @Test void errorStateWithBalanceIsNotSuccessful() throws Exception {
        when(chain.getWalletTokenBalance(any())).thenReturn(response("ERROR", "\"5\""));
        assertThat(service.getBalances(request).tokens()).allMatch(t -> t.balance() == null);
    }

    @Test void repeatRequestsUseSnapshotButChangedWalletGetsFreshBalances() throws Exception {
        when(chain.getWalletTokenBalance(any())).thenReturn(response("OK", "1"));
        var first = service.getBalances(request);
        assertThat(service.getBalances(request)).isSameAs(first);
        verify(chain, times(2)).getWalletTokenBalance(any());
        wallet.updateDaeguWallet(null, "replacement-wallet");
        assertThat(service.getBalances(request)).isNotSameAs(first);
        verify(chain, times(4)).getWalletTokenBalance(any());
    }

    @Test void mismatchedWalletOrContractEchoIsUnavailable() throws Exception {
        var data = new ObjectMapper().readTree("{\"addr\":\"someone-else\",\"balance\":\"1\"}");
        when(chain.getWalletTokenBalance(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, null, data, null));
        assertThat(service.getBalances(request).tokens()).allMatch(t -> t.balance() == null);
    }

    @Test void allTwelveLookupsHaveBoundedConcurrency() throws Exception {
        for (int i = 1; i <= 5; i++) properties.getRewardTokenContracts().put("ESSENTIAL_VIDEO_" + i, "c-e" + i);
        for (int i = 1; i <= 7; i++) properties.getRewardTokenContracts().put("OPTIONAL_VIDEO_" + i, "c-o" + i);
        var active = new java.util.concurrent.atomic.AtomicInteger();
        var maximum = new java.util.concurrent.atomic.AtomicInteger();
        when(chain.getWalletTokenBalance(any())).thenAnswer(call -> {
            int count = active.incrementAndGet();
            maximum.accumulateAndGet(count, Math::max);
            try { Thread.sleep(30); return response("OK", "\"1\""); }
            finally { active.decrementAndGet(); }
        });
        assertThat(service.getBalances(request).tokens()).allMatch(t -> t.status().equals("AVAILABLE"));
        verify(chain, times(12)).getWalletTokenBalance(any());
        assertThat(maximum.get()).isBetween(1, 4);
    }

    private DaeguChainDto.ApiResponse<com.fasterxml.jackson.databind.JsonNode> response(String state, String value) throws Exception {
        return new DaeguChainDto.ApiResponse<>(state, null, null, new ObjectMapper().readTree("{\"balance\":" + value + "}"), null);
    }
}
