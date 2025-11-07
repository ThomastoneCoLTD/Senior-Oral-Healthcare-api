package com.kaii.dentix.domain.subscription.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    /** 기관 ID */
    private Long organizationId;

    /** 기관 이름 */
    private String organizationName;

    /** 변경된 구독 플랜 ID */
    private Long subscriptionPlanId;

    /** 변경된 구독 플랜 이름 */
    private String subscriptionPlanName;

    /** 구독 시작일 */
    private LocalDateTime subscriptionStartDate;

    /** 다음 갱신(리셋) 예정일 */
    private LocalDateTime usageResetDate;

    /** 현재 구독 주기 내 사용량 (successCount) */
    private Integer successCount;
}