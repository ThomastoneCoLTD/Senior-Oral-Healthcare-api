package com.kaii.dentix.domain.organizationSubscriptionHistory.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ 기관 구독 이력 엔티티
 * - 구독 생성, 변경, 갱신 등의 모든 이력을 기록
 * - Organization : SubscriptionHistory = 1 : N
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization_subscription_history")
public class OrganizationSubscriptionHistory extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_history_id")
    private Long id;

    /** ✅ 소속 기관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** ✅ 구독 플랜 정보 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** ✅ 구독 상태 (ACTIVE / EXPIRED / CANCELED 등) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    /** ✅ 구독 시작일 / 종료일 */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /** ✅ 변경 사유 (자동 갱신 / 수동 변경 / 신규 등록 등) */
    @Column(name = "reason", length = 100)
    private String reason;
    @Column(name = "success_count", nullable = false)
    private Integer successCount;
    @Column(name = "remaining_responses", nullable = false)
    private Integer remainingResponses;
    public boolean isOverused() {
        return remainingResponses <= 0;
    }
    public void increaseSuccessCount() {
        this.successCount++;
        this.remainingResponses--;
    }
    /** ✅ 생성자 메서드 */
    public static OrganizationSubscriptionHistory create(Organization organization,
                                                         SubscriptionPlan plan,
                                                         SubscriptionStatus status,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate,
                                                         String reason) {
        return OrganizationSubscriptionHistory.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .reason(reason)
                .build();
    }
}