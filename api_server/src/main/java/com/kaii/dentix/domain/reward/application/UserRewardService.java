package com.kaii.dentix.domain.reward.application;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRewardService {

    private final UserRewardWalletRepository userRewardWalletRepository;
    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final UserRewardProperties userRewardProperties;
    private final JwtTokenUtil jwtTokenUtil;

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

        UserRewardWallet wallet = userRewardWalletRepository.findByUserId(userId)
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
}
