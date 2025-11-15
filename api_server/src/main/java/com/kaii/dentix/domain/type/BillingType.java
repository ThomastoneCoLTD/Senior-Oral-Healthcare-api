package com.kaii.dentix.domain.type;

public enum BillingType {
    MONTHLY, // 정기 청구
    OVERUSE, // 초과 사용 청구
    OVERUSE_BATCH,
    SUBSCRIPTION,
    REGULAR
}
