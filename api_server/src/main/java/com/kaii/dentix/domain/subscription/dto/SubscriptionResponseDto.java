package com.kaii.dentix.domain.subscription.dto;


import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    public static SubscriptionResponseDto from(SubscriptionCycle usage) {
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

    @Entity
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Table(name = "subscription_usage")
    public static class SubscriptionCycle extends TimeEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "usageId")  // ✅ DB 컬럼명 명시
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "organization_id", nullable = false)
        private Organization organization;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "subscription_plan_id", nullable = false)
        private SubscriptionPlan subscriptionPlan;

        @Column(nullable = false)
        private LocalDateTime periodStart; // 구독 시작일

        @Column(nullable = false)
        private LocalDateTime periodEnd; // 다음 갱신일 (monthly/yearly)

        @Column(nullable = false)
        private Integer successCount; // 현재 구독 주기의 사용량

        @Column(nullable = false)
        private Boolean active; // 현재 활성화된 usage 주기 여부

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @PrePersist
        public void onCreate() {
            this.createdAt = LocalDateTime.now();
            if (this.successCount == null) this.successCount = 0;
            if (this.active == null) this.active = true;
        }
    }
}
