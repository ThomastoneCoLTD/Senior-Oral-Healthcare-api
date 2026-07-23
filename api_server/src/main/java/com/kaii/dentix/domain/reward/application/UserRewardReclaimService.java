package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.reward.config.UserRewardProperties;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRewardReclaimService {

    private static final int ESSENTIAL_REWARD_COUNT = 5;
    private static final Set<UserRewardTransactionStatus> COMPLETED_RECLAIM_STATUSES = Set.of(
            UserRewardTransactionStatus.LOCAL_RECORDED,
            UserRewardTransactionStatus.POINT_MINTED,
            UserRewardTransactionStatus.TOKEN_TRANSFERRED
    );

    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final UserRewardWalletRepository userRewardWalletRepository;
    private final ExternalTokenClient externalTokenClient;
    private final DaeguChainProperties daeguChainProperties;
    private final UserRewardProperties userRewardProperties;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public UserRewardDto.ReclaimResponse reclaimOralExerciseTokens(HttpServletRequest request) {
        Long userId = getUserId(request);
        UserRewardWallet wallet = userRewardWalletRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestApiException("reward wallet is not found"));

        List<UserRewardTransaction> allTransactions = userRewardTransactionRepository.findByUserIdOrderByCreatedDesc(userId);
        Set<String> receivedRewardCoinIds = allTransactions.stream()
                .filter(this::isReceivedOralExerciseReward)
                .map(UserRewardTransaction::getCoinId)
                .filter(coinId -> !isBlank(coinId))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        List<String> missingEssentials = IntStream.rangeClosed(1, ESSENTIAL_REWARD_COUNT)
                .mapToObj(index -> "essential_video_" + index)
                .filter(tokenName -> !receivedRewardCoinIds.contains(tokenName))
                .toList();
        if (!missingEssentials.isEmpty()) {
            throw new BadRequestApiException("essential oral exercise tokens are not completed");
        }

        if (!userRewardProperties.isTokenTransferEnabled()) {
            return recordLocalReclaim(userId, wallet, allTransactions);
        }

        if (isBlank(wallet.getDaeguDid()) || isBlank(wallet.getWalletAddress())) {
            throw new BadRequestApiException("reward wallet DID is not connected");
        }
        if (isBlank(daeguChainProperties.getTokenOwnerAddress())) {
            throw new BadRequestApiException("token owner address is not configured");
        }

        List<UserRewardTransaction> rewardTransactions = allTransactions.stream()
                .filter(this::isReclaimableOralExerciseReward)
                .toList();
        if (rewardTransactions.isEmpty()) {
            throw new BadRequestApiException("reclaimable oral exercise token is not found");
        }
        Map<String, String> tokenContracts = getRewardTokenContractsIfNeeded(rewardTransactions);

        List<UserRewardTransaction> reclaimTransactions = new ArrayList<>();
        int skippedCount = 0;
        int failedCount = 0;
        long reclaimedAmount = 0L;

        for (UserRewardTransaction rewardTransaction : rewardTransactions) {
            String tokenContractAddress = resolveTokenContractAddress(rewardTransaction, tokenContracts);
            String idempotencyKey = buildReclaimIdempotencyKey(userId, rewardTransaction);
            UserRewardTransaction reclaimTransaction = userRewardTransactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseGet(() -> userRewardTransactionRepository.save(UserRewardTransaction.builder()
                            .userId(userId)
                            .oralExerciseContent(rewardTransaction.getOralExerciseContent())
                            .type(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM)
                            .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                            .amount(rewardTransaction.getAmount())
                            .balanceAfter(rewardTransaction.getBalanceAfter())
                            .idempotencyKey(idempotencyKey)
                            .sessionId(rewardTransaction.getSessionId())
                            .coinId(rewardTransaction.getCoinId())
                            .tokenContractAddress(tokenContractAddress)
                            .build()));
            String reclaimTokenContractAddress = reclaimTransaction.getTokenContractAddress();
            if (isBlank(reclaimTokenContractAddress) && !isBlank(tokenContractAddress)) {
                reclaimTransaction.updateTokenContractAddress(tokenContractAddress);
                reclaimTokenContractAddress = tokenContractAddress;
            }

            if (reclaimTransaction.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFERRED) {
                skippedCount += 1;
                reclaimTransactions.add(userRewardTransactionRepository.save(reclaimTransaction));
                continue;
            }

            if (isBlank(reclaimTokenContractAddress)) {
                reclaimTransaction.markTokenTransferFailed();
                failedCount += 1;
                reclaimTransactions.add(userRewardTransactionRepository.save(reclaimTransaction));
                continue;
            }

            try {
                JsonNode response = externalTokenClient.transferToken(
                        wallet.getDaeguDid(),
                        normalizeTokenName(rewardTransaction.getCoinId()),
                        reclaimTokenContractAddress,
                        daeguChainProperties.getTokenOwnerAddress(),
                        rewardTransaction.getAmount()
                );
                reclaimTransaction.markTokenTransferred(
                        findFirstText(response, "tx_hash", "transaction_hash", "hash", "Date"),
                        findFirstText(response, "fact_hash")
                );
                reclaimedAmount += rewardTransaction.getAmount();
            } catch (RuntimeException exception) {
                reclaimTransaction.markTokenTransferFailed();
                failedCount += 1;
            }

            reclaimTransactions.add(userRewardTransactionRepository.save(reclaimTransaction));
        }

        int reclaimedCount = (int) reclaimTransactions.stream()
                .filter(transaction -> transaction.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFERRED)
                .count() - skippedCount;
        refreshWalletPointBalance(wallet, allTransactions, reclaimTransactions);

        return UserRewardDto.ReclaimResponse.builder()
                .reclaimedCount(Math.max(reclaimedCount, 0))
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .reclaimedAmount(reclaimedAmount)
                .transactions(reclaimTransactions.stream()
                        .map(UserRewardDto.TransactionResponse::from)
                        .toList())
                .build();
    }

    private UserRewardDto.ReclaimResponse recordLocalReclaim(
            Long userId,
            UserRewardWallet wallet,
            List<UserRewardTransaction> allTransactions
    ) {
        List<UserRewardTransaction> rewardTransactions = allTransactions.stream()
                .filter(this::isReceivedOralExerciseReward)
                .filter(transaction -> !isBlank(transaction.getCoinId()))
                .toList();
        if (rewardTransactions.isEmpty()) {
            throw new BadRequestApiException("reclaimable oral exercise token is not found");
        }

        List<UserRewardTransaction> reclaimTransactions = new ArrayList<>();
        int reclaimedCount = 0;
        int skippedCount = 0;
        long reclaimedAmount = 0L;

        for (UserRewardTransaction rewardTransaction : rewardTransactions) {
            String idempotencyKey = buildReclaimIdempotencyKey(userId, rewardTransaction);
            var existingReclaimTransaction = userRewardTransactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existingReclaimTransaction.isPresent()
                    && existingReclaimTransaction.get().getStatus() != UserRewardTransactionStatus.CANCELED) {
                skippedCount += 1;
                reclaimTransactions.add(existingReclaimTransaction.get());
                continue;
            }

            UserRewardTransaction reclaimTransaction = userRewardTransactionRepository.save(UserRewardTransaction.builder()
                    .userId(userId)
                    .oralExerciseContent(rewardTransaction.getOralExerciseContent())
                    .type(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM)
                    .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                    .amount(rewardTransaction.getAmount())
                    .balanceAfter(rewardTransaction.getBalanceAfter())
                    .idempotencyKey(idempotencyKey)
                    .sessionId(rewardTransaction.getSessionId())
                    .coinId(rewardTransaction.getCoinId())
                    .tokenContractAddress(rewardTransaction.getTokenContractAddress())
                    .build());
            reclaimTransactions.add(reclaimTransaction);
            reclaimedCount += 1;
            reclaimedAmount += rewardTransaction.getAmount();
        }
        refreshWalletPointBalance(wallet, allTransactions, reclaimTransactions);

        return UserRewardDto.ReclaimResponse.builder()
                .reclaimedCount(reclaimedCount)
                .skippedCount(skippedCount)
                .failedCount(0)
                .reclaimedAmount(reclaimedAmount)
                .transactions(reclaimTransactions.stream()
                        .map(UserRewardDto.TransactionResponse::from)
                        .toList())
                .build();
    }

    private boolean isReclaimableOralExerciseReward(UserRewardTransaction transaction) {
        if (transaction.getType() != UserRewardTransactionType.ORAL_EXERCISE_COIN
                || !transaction.isRewardReceived()) {
            return false;
        }
        return transaction.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFERRED
                || transaction.getStatus() == UserRewardTransactionStatus.LOCAL_RECORDED;
    }

    private boolean isReceivedOralExerciseReward(UserRewardTransaction transaction) {
        return transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_COIN
                && transaction.isRewardReceived();
    }

    private boolean isCompletedReclaim(UserRewardTransaction transaction) {
        return transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_RECLAIM
                && COMPLETED_RECLAIM_STATUSES.contains(transaction.getStatus());
    }

    private void refreshWalletPointBalance(
            UserRewardWallet wallet,
            List<UserRewardTransaction> allTransactions,
            List<UserRewardTransaction> reclaimTransactions
    ) {
        long rewardedAmount = allTransactions.stream()
                .filter(this::isReceivedOralExerciseReward)
                .mapToLong(UserRewardTransaction::getAmount)
                .sum();
        Map<String, UserRewardTransaction> completedReclaimsByKey = new LinkedHashMap<>();
        allTransactions.stream()
                .filter(this::isCompletedReclaim)
                .forEach(transaction -> completedReclaimsByKey.put(transaction.getIdempotencyKey(), transaction));
        reclaimTransactions.stream()
                .filter(this::isCompletedReclaim)
                .forEach(transaction -> completedReclaimsByKey.put(transaction.getIdempotencyKey(), transaction));
        long reclaimedAmount = completedReclaimsByKey.values().stream()
                .mapToLong(UserRewardTransaction::getAmount)
                .sum();
        wallet.resetPointBalance(Math.max(rewardedAmount - reclaimedAmount, 0L));
        userRewardWalletRepository.save(wallet);
    }

    private String resolveTokenContractAddress(
            UserRewardTransaction rewardTransaction,
            Map<String, String> tokenContracts
    ) {
        String configuredContractAddress = configuredRewardTokenContract(rewardTransaction.getCoinId());
        if (!isBlank(configuredContractAddress)) {
            if (!contractAddressEquals(configuredContractAddress, rewardTransaction.getTokenContractAddress())) {
                rewardTransaction.updateTokenContractAddress(configuredContractAddress);
                userRewardTransactionRepository.save(rewardTransaction);
            }
            return configuredContractAddress;
        }
        if (!isBlank(rewardTransaction.getTokenContractAddress())) {
            return rewardTransaction.getTokenContractAddress();
        }
        if (isBlank(rewardTransaction.getCoinId())) {
            return null;
        }
        String contractAddress = tokenContracts.get(rewardTransaction.getCoinId().toLowerCase(Locale.ROOT));
        if (!isBlank(contractAddress)) {
            rewardTransaction.updateTokenContractAddress(contractAddress);
            userRewardTransactionRepository.save(rewardTransaction);
            return contractAddress;
        }
        return null;
    }

    private Map<String, String> getRewardTokenContractsIfNeeded(List<UserRewardTransaction> rewardTransactions) {
        List<UserRewardTransaction> missingContractAddressTransactions = rewardTransactions.stream()
                .filter(transaction -> isBlank(transaction.getTokenContractAddress()))
                .toList();
        if (missingContractAddressTransactions.isEmpty()) {
            return Map.of();
        }
        boolean everyMissingContractAddressIsConfigured = missingContractAddressTransactions.stream()
                .filter(transaction -> !isBlank(transaction.getCoinId()))
                .allMatch(transaction -> !isBlank(configuredRewardTokenContract(transaction.getCoinId())));
        if (everyMissingContractAddressIsConfigured) {
            return Map.of();
        }

        JsonNode tokens;
        try {
            tokens = findTokenArray(externalTokenClient.getTokenList());
        } catch (RuntimeException exception) {
            log.warn("Unable to resolve reward token contract addresses for reclaim.", exception);
            return Map.of();
        }
        if (tokens == null || !tokens.isArray()) {
            return Map.of();
        }

        Map<String, String> tokenContracts = new LinkedHashMap<>();
        for (JsonNode token : tokens) {
            String tokenName = findFirstText(token, "token_name", "tokenName", "name");
            String contractAddress = findFirstText(token, "contract", "contract_address", "contractAddress", "cont_addr", "address");
            if (!isBlank(tokenName) && !isBlank(contractAddress) && isAllowedRewardToken(tokenName, contractAddress)) {
                tokenContracts.put(tokenName.toLowerCase(Locale.ROOT), contractAddress);
            }
        }
        return tokenContracts;
    }

    private boolean isAllowedRewardToken(String tokenName, String contractAddress) {
        String configuredContractAddress = configuredRewardTokenContract(tokenName);
        return isBlank(configuredContractAddress)
                || contractAddressEquals(configuredContractAddress, contractAddress);
    }

    private String configuredRewardTokenContract(String tokenName) {
        if (daeguChainProperties.getRewardTokenContracts() == null
                || daeguChainProperties.getRewardTokenContracts().isEmpty()
                || isBlank(tokenName)) {
            return null;
        }
        for (var entry : daeguChainProperties.getRewardTokenContracts().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(tokenName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean contractAddressEquals(String expected, String actual) {
        return !isBlank(expected) && !isBlank(actual) && expected.equalsIgnoreCase(actual);
    }

    private String buildReclaimIdempotencyKey(Long userId, UserRewardTransaction rewardTransaction) {
        Long transactionId = rewardTransaction.getUserRewardTransactionId();
        if (transactionId != null) {
            return "ORAL_EXERCISE_RECLAIM:%d:%d".formatted(userId, transactionId);
        }
        return "ORAL_EXERCISE_RECLAIM:%d:%s".formatted(userId, rewardTransaction.getCoinId());
    }

    private String normalizeTokenName(String tokenName) {
        if (isBlank(tokenName)) {
            throw new BadRequestApiException("reward token name is empty");
        }
        return tokenName.toUpperCase(Locale.ROOT);
    }

    private Long getUserId(HttpServletRequest request) {
        String accessToken = jwtTokenUtil.getAccessToken(request);
        if (accessToken == null) {
            throw new UnauthorizedException("authentication is required");
        }
        return jwtTokenUtil.getUserId(accessToken, TokenType.AccessToken);
    }

    private String findFirstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                String found = findFirstText(fields.next().getValue(), fieldNames);
                if (!isBlank(found)) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findFirstText(child, fieldNames);
                if (!isBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private JsonNode findTokenArray(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        if (payload.isArray()) {
            return payload;
        }
        for (String fieldName : List.of("response", "data", "result", "tokens", "tokenList", "token_list")) {
            JsonNode child = payload.get(fieldName);
            JsonNode found = findTokenArray(child);
            if (found != null && found.isArray()) {
                return found;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
