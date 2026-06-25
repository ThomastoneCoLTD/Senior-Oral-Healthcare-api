package com.kaii.dentix.domain.reward.domain;

public enum UserRewardTransactionStatus {
    LOCAL_RECORDED,
    POINT_MINT_PENDING,
    POINT_MINTED,
    POINT_MINT_FAILED,
    TOKEN_TRANSFERRED,
    TOKEN_TRANSFER_FAILED,
    CANCELED
}
