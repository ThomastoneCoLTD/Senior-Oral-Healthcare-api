package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiLogContext;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/** Read-only chain snapshot. Never provisions a wallet or settles a reward. */
@Service
public class UserWalletTokenBalanceService {
    private static final List<String> TOKEN_NAMES = java.util.stream.Stream.concat(
            IntStream.rangeClosed(1, 5).mapToObj(i -> "ESSENTIAL_VIDEO_" + i),
            IntStream.rangeClosed(1, 7).mapToObj(i -> "OPTIONAL_VIDEO_" + i)).toList();
    private final UserRewardWalletRepository wallets;
    private final JwtTokenUtil jwt;
    private final DaeguChainProperties properties;
    private final DaeguChainClient chain;
    private final Cache<WalletKey, BalanceResponse> cache = Caffeine.newBuilder()
            .maximumSize(2000).expireAfterWrite(Duration.ofSeconds(30)).build();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), runnable -> {
                Thread thread = new Thread(runnable, "wallet-token-balance");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public UserWalletTokenBalanceService(UserRewardWalletRepository wallets, JwtTokenUtil jwt,
                                        DaeguChainProperties properties, DaeguChainClient chain) {
        this.wallets = wallets;
        this.jwt = jwt;
        this.properties = properties;
        this.chain = chain;
    }

    public BalanceResponse getBalances(HttpServletRequest request) {
        String access = jwt.getAccessToken(request);
        if (access == null || access.isBlank()) throw new UnauthorizedException("인증 정보가 없습니다.");
        Long userId = jwt.getUserId(access, TokenType.AccessToken);
        if (userId == null) throw new UnauthorizedException("인증 정보가 없습니다.");
        String address = wallets.findByUserId(userId).map(UserRewardWallet::getWalletAddress).orElse(null);
        if (address == null || address.isBlank()) return new BalanceResponse("NOT_CONNECTED", List.of());
        return cache.get(new WalletKey(userId, address), this::load);
    }

    private BalanceResponse load(WalletKey wallet) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        Map<String, Future<TokenBalance>> tasks = new LinkedHashMap<>();
        List<TokenBalance> tokens = new ArrayList<>();
        try {
            for (String name : TOKEN_NAMES) {
                String contract = properties.getRewardTokenContracts().get(name);
                if (contract == null || contract.isBlank() || properties.resolveUserToken() == null
                        || properties.resolveUserToken().isBlank()) continue;
                try {
                    tasks.put(name, executor.submit(() -> DaeguChainApiLogContext.withUser(wallet.userId(),
                            "wallet-token-balance", () -> lookup(wallet.address(), name, contract))));
                } catch (RejectedExecutionException ignored) { /* Capacity unavailable, never report zero. */ }
            }
            for (String name : TOKEN_NAMES) {
                Future<TokenBalance> task = tasks.get(name);
                TokenBalance result = unavailable(name);
                if (task != null) {
                    try {
                        if (task.isDone()) result = task.get();
                        else {
                            long remaining = deadline - System.nanoTime();
                            if (remaining > 0) result = task.get(remaining, TimeUnit.NANOSECONDS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException | TimeoutException | CancellationException ignored) { }
                }
                tokens.add(result);
            }
            return new BalanceResponse("CONNECTED", List.copyOf(tokens));
        } finally {
            tasks.values().forEach(task -> { if (!task.isDone()) task.cancel(true); });
            executor.purge();
        }
    }

    private TokenBalance lookup(String address, String name, String contract) {
        try {
            var response = chain.getWalletTokenBalance(DaeguChainDto.TokenBalanceApiRequest.builder()
                    .token(properties.resolveUserToken()).chain(properties.getChain()).contAddr(contract).addr(address).build());
            if (response == null || !"OK".equals(response.getState()) || response.getData() == null) return unavailable(name);
            JsonNode data = response.getData();
            // Reject an explicitly mismatched wallet/contract; older responses may omit the echo fields.
            if ((data.has("addr") && !address.equalsIgnoreCase(data.path("addr").asText()))
                    || (data.has("cont_addr") && !contract.equalsIgnoreCase(data.path("cont_addr").asText()))) return unavailable(name);
            JsonNode balance = data.path("balance");
            // Float nodes may already have lost precision in the HTTP JSON decoder.
            if (!balance.isTextual() && !balance.isIntegralNumber()) return unavailable(name);
            String value = balance.asText();
            if (value.length() > 160 || !value.matches("[0-9]+(?:\\.[0-9]+)?")) return unavailable(name);
            return new TokenBalance(name, "AVAILABLE", new BigDecimal(value).stripTrailingZeros().toPlainString(), Instant.now());
        } catch (RuntimeException ignored) {
            return unavailable(name);
        }
    }

    private TokenBalance unavailable(String name) { return new TokenBalance(name, "UNAVAILABLE", null, null); }
    @PreDestroy public void close() { executor.shutdownNow(); }
    private record WalletKey(Long userId, String address) { }
    public record TokenBalance(String tokenName, String status, String balance, Instant checkedAt) { }
    public record BalanceResponse(String status, List<TokenBalance> tokens) { }
}
