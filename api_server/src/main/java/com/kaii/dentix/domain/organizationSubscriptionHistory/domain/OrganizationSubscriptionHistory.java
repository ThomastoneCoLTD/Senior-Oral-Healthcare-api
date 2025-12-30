package com.kaii.dentix.domain.organizationSubscriptionHistory.domain;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.kaii.dentix.global.common.entity.TimeEntity;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;


/**
 * 기관 구독 이력 엔티티
 * 구독 생성, 변경, 갱신 등의 모든 이력을 기록
 * Organization : SubscriptionHistory = 1 : N
 */
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization_subscription_history")
public class OrganizationSubscriptionHistory extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_history_id")
    private Long id;

    /** 소속 기관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** 구독 플랜 정보 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** 구독 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /** 구독 종료일 (활성 구독일 경우 null) */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /** 변경 사유 (자동 갱신 / 수동 변경 / 신규 등록 등) */
    @Column(name = "reason", length = 100)
    private String reason;

    /** 성공 처리된 요청 수 */
    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    /** 남은 요청 수 */
    @Column(name = "remaining_responses", nullable = false)
    private Integer remainingResponses;

    /** 낙관적 락 (동시 요청 보호) */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    /* =====================
     * 비즈니스 메서드
     * ===================== */

    /** 사용량 초과 여부 */
    public boolean isOverused() {
        return remainingResponses <= 0;
    }

    /** 성공 요청 처리 (동시성 안전) */
    public void increaseSuccessCount() {
        this.successCount++;
        this.remainingResponses = Math.max(this.remainingResponses - 1, 0);
    }

    /** 활성 구독 종료 */
    public void expire(LocalDateTime endAt) {
        this.endDate = endAt;
    }

    /* =====================
     * 생성 팩토리
     * ===================== */

    public static OrganizationSubscriptionHistory create(
            Organization organization,
            SubscriptionPlan plan,
            LocalDateTime startDate,
            String reason
    ) {
        return OrganizationSubscriptionHistory.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .startDate(startDate)
                .endDate(null)
                .reason(reason)
                .successCount(0)
                .remainingResponses(
                        plan.getMaxSuccessResponses() != null
                                ? plan.getMaxSuccessResponses()
                                : 0
                )
                .build();
    }
}
