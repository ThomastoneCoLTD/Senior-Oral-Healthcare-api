package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
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
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserRewardService {

    private final UserRewardWalletRepository userRewardWalletRepository;
    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final DaeguChainDidService daeguChainDidService;
    private final DaeguChainPointService daeguChainPointService;
    private final ExternalTokenClient externalTokenClient;
    private final UserRewardProperties userRewardProperties;
    private final JwtTokenUtil jwtTokenUtil;
    private final Environment environment;

    @Transactional(readOnly = true)
    public UserRewardDto.WalletResponse getWallet(HttpServletRequest request) {
        Long userId = getUserId(request);
        return userRewardWalletRepository.findByUserId(userId)
                .map(wallet -> UserRewardDto.WalletResponse.builder()
                        .pointBalance(wallet.getPointBalance())
                        .daeguDid(wallet.getDaeguDid())
                        .walletAddress(wallet.getWalletAddress())
                        .build())
                .orElseGet(UserRewardDto.WalletResponse::empty);
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
            return UserRewardDto.RewardResponse.from(existingTokenReward.get(), true);
        }

        String idempotencyKey = buildButtonClickIdempotencyKey(userId, rewardTokenName);
        var existingIdempotentTransaction = userRewardTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingIdempotentTransaction.isPresent() && existingIdempotentTransaction.get().isAlreadyApplied()) {
            return UserRewardDto.RewardResponse.from(existingIdempotentTransaction.get(), true);
        }

        UserRewardWallet wallet = userRewardWalletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> UserRewardWallet.builder()
                        .userId(userId)
                        .pointBalance(0L)
                        .build());

        long amount = userRewardProperties.getOralExerciseCoinAmount();
        wallet.addPoints(amount);
        UserRewardWallet savedWallet = userRewardWalletRepository.save(wallet);

        UserRewardTransaction transaction = userRewardTransactionRepository.save(UserRewardTransaction.builder()
                .userId(userId)
                .oralExerciseContent(content)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.LOCAL_RECORDED)
                .amount(amount)
                .balanceAfter(savedWallet.getPointBalance())
                .idempotencyKey(idempotencyKey)
                .sessionId(buttonClickRequest.getSessionId())
                .coinId(rewardTokenName)
                .build());

        if (userRewardProperties.isTokenTransferEnabled()) {
            transferTokenIfConfigured(transaction, savedWallet, rewardTokenName);
        } else {
            mintPointIfConfigured(transaction, savedWallet);
        }

        return UserRewardDto.RewardResponse.from(transaction, false);
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
            return;
        }

        try {
            JsonNode response = externalTokenClient.transferToken(
                    wallet.getDaeguDid(),
                    rewardTokenName,
                    wallet.getWalletAddress(),
                    transaction.getAmount()
            );
            transaction.markTokenTransferred(
                    findFirstText(response, "tx_hash", "transaction_hash", "hash", "Date"),
                    findFirstText(response, "fact_hash")
            );
        } catch (RuntimeException exception) {
            transaction.markTokenTransferFailed();
        }
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
        return index < 0 || index == did.length() - 1 ? null : did.substring(index + 1);
    }

    private record DidWallet(String did, String walletAddress) {
    }
}
