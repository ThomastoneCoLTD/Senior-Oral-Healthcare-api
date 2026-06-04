package com.kaii.dentix.domain.reward.dto;

import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import lombok.*;

public class UserRewardDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoinClickRequest {
        private Long contentId;
        private String sessionId;
        private String coinId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardResponse {
        private long amount;
        private long pointBalance;
        private boolean duplicated;
        private UserRewardTransactionStatus status;
        private Long transactionId;

        public static RewardResponse from(UserRewardTransaction transaction, boolean duplicated) {
            return RewardResponse.builder()
                    .amount(transaction.getAmount())
                    .pointBalance(transaction.getBalanceAfter())
                    .duplicated(duplicated)
                    .status(transaction.getStatus())
                    .transactionId(transaction.getUserRewardTransactionId())
                    .build();
        }
    }
}
