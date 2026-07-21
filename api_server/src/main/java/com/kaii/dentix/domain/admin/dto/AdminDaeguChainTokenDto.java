package com.kaii.dentix.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.Date;
import java.util.List;

public class AdminDaeguChainTokenDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenOption {
        private String tokenName;
        private String contractAddress;
        private String symbol;
        private Long supply;
        private Integer decimals;
        private String owner;
        private String issued;
        private String txHash;
        private String factHash;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "tokenName is required")
        @JsonAlias("tokenName")
        @JsonProperty("token_name")
        private String tokenName;

        @NotNull(message = "supply is required")
        @Positive(message = "supply must be positive")
        private Long supply;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardTransfer {
        private Long transactionId;
        private Long userId;
        private String userLoginIdentifier;
        private String userName;
        private String contentTitle;
        private String tokenName;
        private String tokenContractAddress;
        private UserRewardTransactionStatus status;
        private long amount;
        private long balanceAfter;
        private String sessionId;
        private String txHash;
        private String factHash;
        private Date created;

        public static RewardTransfer from(UserRewardTransaction transaction, User user) {
            OralExerciseContent content = transaction.getOralExerciseContent();
            return RewardTransfer.builder()
                    .transactionId(transaction.getUserRewardTransactionId())
                    .userId(transaction.getUserId())
                    .userLoginIdentifier(user == null ? null : user.getUserLoginIdentifier())
                    .userName(user == null ? null : user.getUserName())
                    .contentTitle(content == null ? null : content.getTitle())
                    .tokenName(transaction.getCoinId())
                    .tokenContractAddress(transaction.getTokenContractAddress())
                    .status(transaction.getStatus())
                    .amount(transaction.getAmount())
                    .balanceAfter(transaction.getBalanceAfter())
                    .sessionId(transaction.getSessionId())
                    .txHash(transaction.getDaeguChainTxHash())
                    .factHash(transaction.getDaeguChainFactHash())
                    .created(transaction.getCreated())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardTransferListResponse {
        private List<RewardTransfer> transfers;
    }
}
