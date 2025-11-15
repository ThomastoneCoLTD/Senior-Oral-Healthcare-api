package com.kaii.dentix.domain.type;

public enum SubscriptionStatus {
    PENDING,    // 결제 대기
    ACTIVE,     // 활성 구독 중
    SUSPENDED,  // 결제 실패, 임시 중단
    CANCELED,   // 사용자가 해지
    EXPIRED,    // 기간 만료
    FAILED      // 결제 실패 등 시스템 에러
}
