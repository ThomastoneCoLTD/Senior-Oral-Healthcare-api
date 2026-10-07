package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AdminRewardTransferRecoveryDto {
    private AdminRewardTransferRecoveryDto() {}

    public record Request(
            @NotBlank(message = "거래 fact hash가 필요합니다.")
            @Pattern(regexp = "[A-Za-z0-9]{32,100}", message = "거래 fact hash 형식이 올바르지 않습니다.")
            String factHash) {}

    public record Preview(Long transactionId, String factHash, String sender, String receiver,
                          String tokenContractAddress, long amount, long height,
                          UserRewardTransactionStatus status) {}
}
