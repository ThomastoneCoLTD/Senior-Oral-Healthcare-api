package com.kaii.dentix.domain.subscriptionInfo.dto;


import com.kaii.dentix.domain.subscriptionInfo.domain.SubscriptionUsage;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.reactivestreams.Subscription;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponseDto {

    private Long id;
    private String organizationName;
    private String planName;
    private String planCycle;
    private Integer maxSuccessResponses;
    private Integer successCount;
    private Double usageRate;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Boolean active;

    public static SubscriptionResponseDto from(SubscriptionUsage usage) {
        SubscriptionPlan plan = usage.getSubscriptionPlan();

        // ✅ 사용률 계산 (0~1 비율)
        double usageRate = 0.0;
        if (plan.getMaxSuccessResponses() != null && plan.getMaxSuccessResponses() > 0) {
            usageRate = (double) usage.getSuccessCount() / plan.getMaxSuccessResponses();
        }

        return SubscriptionResponseDto.builder()
                .id(usage.getId())
                .organizationName(usage.getOrganization().getOrganizationName())
                .planName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .maxSuccessResponses(plan.getMaxSuccessResponses())
                .successCount(usage.getSuccessCount())
                .usageRate(usageRate)
                .periodStart(usage.getPeriodStart())
                .periodEnd(usage.getPeriodEnd())
                .active(usage.getActive())
                .build();
    }
}
