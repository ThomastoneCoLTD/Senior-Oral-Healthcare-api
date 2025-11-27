package com.kaii.dentix.domain.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BillingListResponse {

    private List<BillingResponse> billings;  // 상세 리스트
    private Long totalAmount;              // 합계 금액
}