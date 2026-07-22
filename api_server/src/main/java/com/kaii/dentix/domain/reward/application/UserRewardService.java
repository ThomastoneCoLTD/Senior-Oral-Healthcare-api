package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.reward.config.UserRewardProperties;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.OralExerciseRewardToken;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserRewardService {

    private static final EnumSet<UserRewardTransactionStatus> NON_REWARDED_STATUSES = EnumSet.of(
            UserRewardTransactionStatus.CANCELED,
            UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED,
            UserRewardTransactionStatus.POINT_MINT_FAILED
    );

    private final UserRewardWalletRepository userRewardWalletRepository;
    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final DaeguChainAccountService daeguChainAccountService;
    private final DaeguChainDidService daeguChainDidService;
    private final DaeguChainPointService daeguChainPointService;
    private final ExternalTokenClient externalTokenClient;
    private final UserRepository userRepository;
    private final UserRewardProperties userRewardProperties;
    private final DaeguChainProperties daeguChainProperties;
    private final JwtTokenUtil jwtTokenUtil;
    private final Environment environment;

    @Transactional
    public UserRewardDto.WalletResponse getWallet(HttpServletRequest request) {
        Long userId = getUserId(request);
        UserRewardWallet wallet = getOrCreateRewardWallet(userId);
        return UserRewardDto.WalletResponse.builder()
                .pointBalance(wallet.getPointBalance())
                .daeguDid(wallet.getDaeguDid())
                .walletAddress(wallet.getWalletAddress())
                .build();
    }

    @Transactional(readOnly = true)
    public UserRewardDto.TransactionListResponse getTransactions(HttpServletRequest request) {
        Long userId = getUserId(request);
        return UserRewardDto.TransactionListResponse.builder()
                .transactions(userRewardTransactionRepository.findByUserIdOrderByCreatedDesc(userId)
                        .stream()
                        .map(UserRewardDto.TransactionResponse::from)
                        .toList())
                .build();
    }

    @Transactional
    public UserRewardDto.WalletResponse connectWallet(
            HttpServletRequest request,
            UserRewardDto.WalletConnectRequest connectRequest
    ) {
        Long userId = getUserId(request);
        UserRewardWallet wallet = userRewardWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> UserRewardWallet.builder()
                        .userId(userId)
                        .pointBalance(0L)
                        .build());

        String daeguDid = connectRequest == null ? null : connectRequest.getDaeguDid();
        String walletAddress = connectRequest == null ? null : connectRequest.getWalletAddress();

        if (isBlank(walletAddress) && !isBlank(daeguDid)) {
            walletAddress = extractAddressFromDid(daeguDid);
        }

        if (isBlank(wallet.getWalletAddress()) && isBlank(walletAddress)) {
            DidWallet didWallet = createDidWallet(userId);
            if (isBlank(daeguDid)) {
                daeguDid = didWallet.did();
            }
            walletAddress = didWallet.walletAddress();
        }

        if (isBlank(walletAddress) && isBlank(wallet.getWalletAddress())) {
            throw new BadRequestApiException("walletAddress is required");
        }

        wallet.updateDaeguWallet(daeguDid, walletAddress);
        resetWalletToRewardedPointBalance(userId, wallet);
        UserRewardWallet savedWallet = userRewardWalletRepository.save(wallet);

        return UserRewardDto.WalletResponse.builder()
                .pointBalance(savedWallet.getPointBalance())
                .daeguDid(savedWallet.getDaeguDid())
                .walletAddress(savedWallet.getWalletAddress())
                .build();
    }

    @Transactional
    public UserRewardDto.RewardResponse rewardOralExerciseButtonClick(
            HttpServletRequest request,
            UserRewardDto.ButtonClickRequest buttonClickRequest
    ) {
        Long userId = getUserId(request);
        validateButtonClickRequest(buttonClickRequest);
        OralExerciseContent content = oralExerciseContentRepository
                .findById(buttonClickRequest.getContentId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 구강체조 콘텐츠입니다."));
        String rewardTokenName = resolveRewardTokenName(content);

        var existingTokenReward = userRewardTransactionRepository
                .findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
                        userId,
                        rewardTokenName,
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.CANCELED
        );
        if (existingTokenReward.isPresent()) {
            return retryTokenTransferIfNeeded(userId, existingTokenReward.get(), rewardTokenName);
        }

        String idempotencyKey = buildButtonClickIdempotencyKey(userId, rewardTokenName);
        var existingIdempotentTransaction = userRewardTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingIdempotentTransaction.isPresent() && existingIdempotentTransaction.get().isAlreadyApplied()) {
            return retryTokenTransferIfNeeded(userId, existingIdempotentTransaction.get(), rewardTokenName);
        }

        UserRewardWallet wallet = getOrCreateRewardWallet(userId);
        long amount = userRewardProperties.getOralExerciseCoinAmount();

        UserRewardTransaction transaction = userRewardTransactionRepository.save(UserRewardTransaction.builder()
                .userId(userId)
                .oralExerciseContent(content)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                .amount(amount)
                .balanceAfter(wallet.getPointBalance())
                .idempotencyKey(idempotencyKey)
                .sessionId(buttonClickRequest.getSessionId())
                .coinId(rewardTokenName)
                .build());

        if (userRewardProperties.isTokenTransferEnabled()) {
            transferTokenIfConfigured(transaction, wallet, rewardTokenName);
        } else {
            mintPointIfConfigured(transaction, wallet);
        }

        assertRewardSucceeded(transaction);
        creditReward(wallet, transaction);
        return UserRewardDto.RewardResponse.from(transaction, false, wallet.getPointBalance());
    }

    private UserRewardDto.RewardResponse retryTokenTransferIfNeeded(
            Long userId,
            UserRewardTransaction transaction,
            String rewardTokenName
    ) {
        UserRewardWallet wallet = getOrCreateRewardWallet(userId);
        if (userRewardProperties.isTokenTransferEnabled()
                && transaction.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED) {
            transferTokenIfConfigured(transaction, wallet, rewardTokenName);
            assertRewardSucceeded(transaction);
            creditReward(wallet, transaction);
            return UserRewardDto.RewardResponse.from(transaction, true, wallet.getPointBalance());
        }
        assertRewardSucceeded(transaction);
        return UserRewardDto.RewardResponse.from(transaction, true, wallet.getPointBalance());
    }

    private UserRewardWallet getOrCreateRewardWallet(Long userId) {
        UserRewardWallet wallet = userRewardWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> UserRewardWallet.builder()
                        .userId(userId)
                        .pointBalance(0L)
                        .build());
        syncWalletFromUser(userId, wallet);
        resetWalletToRewardedPointBalance(userId, wallet);
        return userRewardWalletRepository.save(wallet);
    }

    private void resetWalletToRewardedPointBalance(Long userId, UserRewardWallet wallet) {
        wallet.resetPointBalance(calculateRewardedPointBalance(userId));
    }

    private long calculateRewardedPointBalance(Long userId) {
        return userRewardTransactionRepository.sumRewardedAmount(
                userId,
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                NON_REWARDED_STATUSES
        );
    }

    private void creditReward(UserRewardWallet wallet, UserRewardTransaction transaction) {
        if (!transaction.isRewardReceived()) {
            return;
        }
        wallet.addPoints(transaction.getAmount());
        transaction.updateBalanceAfter(wallet.getPointBalance());
        userRewardWalletRepository.save(wallet);
    }

    private void syncWalletFromUser(Long userId, UserRewardWallet wallet) {
        if (!isBlank(wallet.getDaeguDid()) && !isBlank(wallet.getWalletAddress())) {
            return;
        }
        userRepository.findById(userId)
                .filter(user -> user.getDaeguDidStatus() == UserDaeguIdentityStatus.ISSUED)
                .ifPresent(user -> {
                    String daeguDid = isBlank(wallet.getDaeguDid()) ? user.getDaeguDid() : wallet.getDaeguDid();
                    String walletAddress = isBlank(wallet.getWalletAddress())
                            ? extractWalletAddress(user)
                            : wallet.getWalletAddress();
                    wallet.updateDaeguWallet(daeguDid, walletAddress);
                });
    }

    private String extractWalletAddress(User user) {
        if (user == null) {
            return null;
        }
        String walletAddress = extractAddressFromDid(user.getDaeguDid());
        if (!isBlank(walletAddress)) {
            return walletAddress;
        }
        return extractAddressFromDid(user.getDaeguDidKey());
    }

    private Long getUserId(HttpServletRequest request) {
        String accessToken = jwtTokenUtil.getAccessToken(request);
        if (accessToken == null) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }
        return jwtTokenUtil.getUserId(accessToken, TokenType.AccessToken);
    }

    private void validateButtonClickRequest(UserRewardDto.ButtonClickRequest request) {
        if (request == null) {
            throw new BadRequestApiException("request is required");
        }
        if (request.getContentId() == null) {
            throw new BadRequestApiException("contentId is required");
        }
        if (request.getSelectedButtonNumber() == null) {
            throw new BadRequestApiException("selectedButtonNumber is required");
        }
        if (request.getSelectedButtonNumber() < 1 || request.getSelectedButtonNumber() > 5) {
            throw new BadRequestApiException("selectedButtonNumber must be between 1 and 5");
        }
        if (request.getTargetButtonNumber() != null
                && !request.getTargetButtonNumber().equals(request.getSelectedButtonNumber())) {
            throw new BadRequestApiException("selectedButtonNumber does not match targetButtonNumber");
        }
    }

    private String buildButtonClickIdempotencyKey(Long userId, String rewardTokenName) {
        return "ORAL_EXERCISE_BUTTON:%d:%s".formatted(
                userId,
                rewardTokenName
        );
    }

    private String resolveRewardTokenName(OralExerciseContent content) {
        String tokenName = OralExerciseRewardToken.tokenNameForContentSort(content.getContentSort());
        if (isBlank(tokenName)) {
            throw new BadRequestApiException("reward token name is not configured for content");
        }
        return tokenName;
    }

    private void transferTokenIfConfigured(UserRewardTransaction transaction, UserRewardWallet wallet, String rewardTokenName) {
        if (isBlank(wallet.getDaeguDid())
                || isBlank(wallet.getWalletAddress())) {
            transaction.markTokenTransferFailed();
            throwRewardFailure();
            return;
        }

        try {
            RewardTokenRef rewardToken = resolveRewardTokenRef(transaction, rewardTokenName);
            transaction.updateTokenContractAddress(rewardToken.contractAddress());
            JsonNode response = externalTokenClient.transferToken(
                    wallet.getDaeguDid(),
                    rewardToken.tokenName(),
                    rewardToken.contractAddress(),
                    wallet.getWalletAddress(),
                    transaction.getAmount()
            );
            transaction.markTokenTransferred(
                    findFirstText(response, "tx_hash", "transaction_hash", "hash", "Date"),
                    findFirstText(response, "fact_hash")
            );
        } catch (RuntimeException exception) {
            transaction.markTokenTransferFailed();
            throwRewardFailure();
        }
    }

    private RewardTokenRef resolveRewardTokenRef(UserRewardTransaction transaction, String rewardTokenName) {
        String configuredContractAddress = configuredRewardTokenContract(rewardTokenName);
        if (!isBlank(configuredContractAddress)) {
            return new RewardTokenRef(rewardTokenName.toUpperCase(java.util.Locale.ROOT), configuredContractAddress);
        }

        if (!isBlank(transaction.getTokenContractAddress())) {
            return new RewardTokenRef(rewardTokenName, transaction.getTokenContractAddress());
        }

        JsonNode tokens = findTokenArray(externalTokenClient.getTokenList());
        if (tokens != null && tokens.isArray()) {
            for (JsonNode token : tokens) {
                String tokenName = findFirstText(token, "token_name", "tokenName", "name");
                String contractAddress = findFirstText(token, "contract", "contract_address", "contractAddress", "cont_addr", "address");
                if (!isBlank(tokenName)
                        && !isBlank(contractAddress)
                        && tokenName.equalsIgnoreCase(rewardTokenName)
                        && isAllowedRewardToken(tokenName, contractAddress)) {
                    return new RewardTokenRef(tokenName, contractAddress);
                }
            }
        }

        throw new BadRequestApiException("reward token contract address is not found: " + rewardTokenName);
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

    private void assertRewardSucceeded(UserRewardTransaction transaction) {
        if (transaction.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED
                || transaction.getStatus() == UserRewardTransactionStatus.POINT_MINT_FAILED) {
            throwRewardFailure();
        }
    }

    private void throwRewardFailure() {
        throw new BadRequestApiException("토큰 지급에 실패했습니다. 보상을 받을 수 없습니다.");
    }

    private void mintPointIfConfigured(UserRewardTransaction transaction, UserRewardWallet wallet) {
        if (!userRewardProperties.isPointMintEnabled()) {
            return;
        }
        if (isBlank(userRewardProperties.getPointContractAddress())
                || isBlank(userRewardProperties.getPointOwnerAddress())
                || isBlank(userRewardProperties.getPointOwnerPrivateKey())
                || isBlank(wallet.getWalletAddress())) {
            transaction.markPointMintFailed();
            return;
        }

        try {
            DaeguChainDto.ApiResponse<com.fasterxml.jackson.databind.JsonNode> response =
                    daeguChainPointService.mintPoint(new DaeguChainDto.TokenMintRequest(
                            null,
                            null,
                            userRewardProperties.getPointContractAddress(),
                            userRewardProperties.getPointOwnerAddress(),
                            userRewardProperties.getPointOwnerPrivateKey(),
                            wallet.getWalletAddress(),
                            String.valueOf(transaction.getAmount())
                    ));
            transaction.markPointMinted(extractHash(response, "tx_hash", "transaction_hash", "hash"), extractHash(response, "fact_hash"));
        } catch (RuntimeException exception) {
            transaction.markPointMintFailed();
        }
    }

    private String extractHash(DaeguChainDto.ApiResponse<JsonNode> response, String... fields) {
        if (response == null || response.getData() == null) {
            return response == null ? null : response.getCid();
        }
        for (String field : fields) {
            String value = findFirstText(response.getData(), field);
            if (!isBlank(value)) {
                return value;
            }
        }
        return response.getCid();
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

    private DidWallet createDidWallet(Long userId) {
        try {
            DaeguChainDto.ApiResponse<JsonNode> response = daeguChainDidService.createAccount(Map.of());
            JsonNode data = response == null ? null : response.getData();
            String did = findFirstText(data, "did", "DID", "account");
            String walletAddress = findFirstText(data, "address");
            if (isBlank(walletAddress)) {
                walletAddress = extractAddressFromDid(did);
            }
            if (isBlank(walletAddress)) {
                walletAddress = createDaeguWalletAddress();
            }
            if (isBlank(walletAddress)) {
                throw new BadRequestApiException("DaeguChain DID wallet address is empty");
            }
            return new DidWallet(did, walletAddress);
        } catch (BadRequestApiException exception) {
            if (isDevProfile() && exception.getMessage() != null && exception.getMessage().contains("token is required")) {
                return new DidWallet(null, buildLocalTestWalletAddress(userId));
            }
            throw exception;
        }
    }

    private String buildLocalTestWalletAddress(Long userId) {
        long hash = Integer.toUnsignedLong(Objects.hash("soh-local-wallet", userId));
        return "0x" + "%040x".formatted(hash);
    }

    private String createDaeguWalletAddress() {
        DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> response =
                daeguChainAccountService.createAccount(new DaeguChainDto.AccountCreateRequest(null, null));
        return response == null
                || response.getData() == null
                || response.getData().getKeyPair() == null
                ? null
                : response.getData().getKeyPair().getAddress();
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev") || profile.equals("local"));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String extractAddressFromDid(String did) {
        if (isBlank(did)) {
            return null;
        }
        int index = did.lastIndexOf(':');
        String candidate = index < 0 || index == did.length() - 1 ? null : did.substring(index + 1);
        return candidate != null && candidate.startsWith("0x") ? candidate : null;
    }

    private record DidWallet(String did, String walletAddress) {
    }

    private record RewardTokenRef(String tokenName, String contractAddress) {
    }
}
