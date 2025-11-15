package com.kaii.dentix.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingStatusUpdateRequest {

    @NotBlank(message = "변경할 상태는 필수입니다.")
    private String billingStatus; // "PAID", "UNPAID", "PENDING"

    private String memo; // 선택 메모
}