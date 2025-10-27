package com.kaii.dentix.domain.organization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSubscriptionResponse {

    /** ✅ 기관 기본 정보 */
    private Long organizationId;
    private String organizationName;
    private String organizationPhoneNumber;

    /** ✅ 구독 상품 정보 */
    private Long subscriptionPlanId;
    private String subscriptionPlanName;
    private String planCycle;           // MONTHLY / YEARLY
    private Long price;                  // 가격
    private int maxSuccessResponses;    // 제공량 (최대 응답수)

    /** ✅ 사용 현황 */
    private int successCount;           // 총 사용 횟수
    private int remainingCount;         // 남은 횟수
    private double usageRate;           // 사용률 (%)

    /** ✅ 날짜 정보 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate subscriptionStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate usageResetDate;

    /** ✅ 생성자 (엔티티 → DTO 변환용) */
    public static OrganizationSubscriptionResponse fromEntity(
            Long organizationId,
            String organizationName,
            String organizationPhoneNumber,
            SubscriptionPlan plan,
            int successCount,
            LocalDate subscriptionStartDate,
            LocalDate usageResetDate
    ) {
        int remaining = Math.max(plan.getMaxSuccessResponses() - successCount, 0);
        double usage = plan.getMaxSuccessResponses() > 0
                ? (double) successCount / plan.getMaxSuccessResponses() * 100
                : 0.0;

        return OrganizationSubscriptionResponse.builder()
                .organizationId(organizationId)
                .organizationName(organizationName)
                .organizationPhoneNumber(organizationPhoneNumber)
                .subscriptionPlanId(plan.getId())
                .subscriptionPlanName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .price(plan.getPrice())
                .maxSuccessResponses(plan.getMaxSuccessResponses())
                .successCount(successCount)
                .remainingCount(remaining)
                .usageRate(Math.round(usage * 10) / 10.0) // 소수점 1자리 반올림
                .subscriptionStartDate(subscriptionStartDate)
                .usageResetDate(usageResetDate)
                .build();
    }
}