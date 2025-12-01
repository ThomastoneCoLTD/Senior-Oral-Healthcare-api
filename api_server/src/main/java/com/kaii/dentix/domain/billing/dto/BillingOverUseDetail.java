package com.kaii.dentix.domain.billing.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingOverUseDetail {

    private Long overuseBillingId;    // 개별 초과 Billing ID
    private Integer amount;           // 금액
    private LocalDateTime createdAt;  // 발생일
    private String description;       // 메모 또는 설명
}