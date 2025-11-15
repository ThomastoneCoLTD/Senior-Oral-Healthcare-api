package com.kaii.dentix.domain.organization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ✅ 기관 + 구독 정보 DTO
 *  - 기관 기본 정보
 *  - 구독 상품 정보
 *  - 사용 현황
 *  - 구독 기간 정보 포함
 */
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
    private String planCycle;           // monthly / yearly
    private Double price;               // 구독 요금
    private int maxSuccessResponses;    // 기본 제공량 (최대 응답수)

    /** ✅ 사용 현황 */
    private int successCount;           // 총 사용 횟수
    private int remainingCount;         // 남은 횟수
    private double usageRate;           // 사용률 (%)

    /** ✅ 날짜 정보 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate subscriptionStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate subscriptionEndDate; // ✅ 추가 (구독 종료일)

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate usageResetDate;

    /**
     * ✅ Entity → DTO 변환 메서드
     */
    public static OrganizationSubscriptionResponse fromEntity(
            Long organizationId,
            String organizationName,
            String organizationPhoneNumber,
            SubscriptionPlan plan,
            int successCount,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            LocalDate usageResetDate
    ) {
        // 남은 사용량
        int remaining = Math.max(plan.getMaxSuccessResponses() - successCount, 0);

        // 사용률 계산
        double usageRate = 0.0;
        if (plan.getMaxSuccessResponses() > 0) {
            usageRate = ((double) successCount / plan.getMaxSuccessResponses()) * 100;
            // 남은 수량이 0이면 무조건 100%
            if (remaining == 0) {
                usageRate = 100.0;
            }
        }

        // 소수점 한 자리 반올림
        usageRate = Math.round(usageRate * 10) / 10.0;

        return OrganizationSubscriptionResponse.builder()
                .organizationId(organizationId)
                .organizationName(organizationName)
                .organizationPhoneNumber(organizationPhoneNumber)
                .subscriptionPlanId(plan.getId())
                .subscriptionPlanName(plan.getPlanName().name())
                .planCycle(plan.getPlanCycle())
                .price(plan.getPrice())
                .maxSuccessResponses(plan.getMaxSuccessResponses())
                .successCount(successCount)
                .remainingCount(remaining)
                .usageRate(usageRate)
                .subscriptionStartDate(subscriptionStartDate)
                .subscriptionEndDate(subscriptionEndDate)
                .usageResetDate(usageResetDate)
                .build();
    }
}