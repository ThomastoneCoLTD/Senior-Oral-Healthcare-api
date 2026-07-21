package com.kaii.dentix.domain.reward.dto;

import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import lombok.*;

import java.util.Date;
import java.util.List;

public class UserRewardDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ButtonClickRequest {
        private Long contentId;
        private String sessionId;
        private Integer selectedButtonNumber;
        private Integer targetButtonNumber;
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
            return from(transaction, duplicated, transaction.getBalanceAfter());
        }

        public static RewardResponse from(UserRewardTransaction transaction, boolean duplicated, long pointBalance) {
            return RewardResponse.builder()
                    .amount(transaction.getAmount())
                    .pointBalance(pointBalance)
                    .duplicated(duplicated)
                    .status(transaction.getStatus())
                    .transactionId(transaction.getUserRewardTransactionId())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletResponse {
        private long pointBalance;
        private String daeguDid;
        private String walletAddress;

        public static WalletResponse empty() {
            return WalletResponse.builder()
                    .pointBalance(0L)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletConnectRequest {
        private String daeguDid;
        private String walletAddress;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private String type;
        private UserRewardTransactionStatus status;
        private long amount;
        private long balanceAfter;
        private String contentTitle;
        private String sessionId;
        private String coinId;
        private String tokenContractAddress;
        private String daeguChainTxHash;
        private String daeguChainFactHash;
        private Date created;

        public static TransactionResponse from(UserRewardTransaction transaction) {
            return TransactionResponse.builder()
                    .id(transaction.getUserRewardTransactionId())
                    .type(transaction.getType().name())
                    .status(transaction.getStatus())
                    .amount(transaction.isRewardReceived() ? transaction.getAmount() : 0L)
                    .balanceAfter(transaction.getBalanceAfter())
                    .contentTitle(transaction.getOralExerciseContent() == null
                            ? null
                            : transaction.getOralExerciseContent().getTitle())
                    .sessionId(transaction.getSessionId())
                    .coinId(transaction.getCoinId())
                    .tokenContractAddress(transaction.getTokenContractAddress())
                    .daeguChainTxHash(transaction.getDaeguChainTxHash())
                    .daeguChainFactHash(transaction.getDaeguChainFactHash())
                    .created(transaction.getCreated())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionListResponse {
        private List<TransactionResponse> transactions;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReclaimResponse {
        private int reclaimedCount;
        private int skippedCount;
        private int failedCount;
        private long reclaimedAmount;
        private List<TransactionResponse> transactions;
    }
}
