package com.kaii.dentix.domain.reward.application;

import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.reward.config.UserRewardProperties;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserRewardService {

    private final UserRewardWalletRepository userRewardWalletRepository;
    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final DaeguChainAccountService daeguChainAccountService;
    private final DaeguChainPointService daeguChainPointService;
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

        if (isBlank(wallet.getWalletAddress()) && isBlank(walletAddress)) {
            walletAddress = createDaeguWalletAddress(userId);
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
    public UserRewardDto.RewardResponse rewardOralExerciseCoin(
            HttpServletRequest request,
            UserRewardDto.CoinClickRequest coinClickRequest
    ) {
        Long userId = getUserId(request);
        validateCoinClickRequest(coinClickRequest);
        OralExerciseContent content = oralExerciseContentRepository
                .findById(coinClickRequest.getContentId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 구강체조 콘텐츠입니다."));

        String idempotencyKey = buildIdempotencyKey(userId, coinClickRequest);
        var existingTransaction = userRewardTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransaction.isPresent() && existingTransaction.get().isAlreadyApplied()) {
            return UserRewardDto.RewardResponse.from(existingTransaction.get(), true);
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
                .sessionId(coinClickRequest.getSessionId())
                .coinId(coinClickRequest.getCoinId())
                .build());

        mintPointIfConfigured(transaction, savedWallet);

        return UserRewardDto.RewardResponse.from(transaction, false);
    }

    private Long getUserId(HttpServletRequest request) {
        String accessToken = jwtTokenUtil.getAccessToken(request);
        if (accessToken == null) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }
        return jwtTokenUtil.getUserId(accessToken, TokenType.AccessToken);
    }

    private void validateCoinClickRequest(UserRewardDto.CoinClickRequest request) {
        if (request == null) {
            throw new BadRequestApiException("request is required");
        }
        if (request.getContentId() == null) {
            throw new BadRequestApiException("contentId is required");
        }
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BadRequestApiException("sessionId is required");
        }
        if (request.getCoinId() == null || request.getCoinId().isBlank()) {
            throw new BadRequestApiException("coinId is required");
        }
    }

    private String buildIdempotencyKey(Long userId, UserRewardDto.CoinClickRequest request) {
        return "ORAL_EXERCISE_COIN:%d:%d:%s:%s".formatted(
                userId,
                request.getContentId(),
                request.getSessionId(),
                request.getCoinId()
        );
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

    private String extractHash(DaeguChainDto.ApiResponse<com.fasterxml.jackson.databind.JsonNode> response, String... fields) {
        if (response == null || response.getData() == null) {
            return response == null ? null : response.getCid();
        }
        for (String field : fields) {
            com.fasterxml.jackson.databind.JsonNode value = response.getData().get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return response.getCid();
    }

    private String extractWalletAddress(DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> account) {
        if (account == null
                || account.getData() == null
                || account.getData().getKeyPair() == null
                || isBlank(account.getData().getKeyPair().getAddress())) {
            throw new BadRequestApiException("DaeguChain account address is empty");
        }
        return account.getData().getKeyPair().getAddress();
    }

    private String createDaeguWalletAddress(Long userId) {
        try {
            DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> account =
                    daeguChainAccountService.createAccount(new DaeguChainDto.AccountCreateRequest(null, null));
            return extractWalletAddress(account);
        } catch (BadRequestApiException exception) {
            if (isDevProfile() && exception.getMessage() != null && exception.getMessage().contains("token is required")) {
                return buildLocalTestWalletAddress(userId);
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
}
