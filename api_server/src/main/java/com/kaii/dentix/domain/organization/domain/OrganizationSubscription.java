package com.kaii.dentix.domain.organization.domain;

import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ 기관 구독 정보 (OrganizationSubscription)
 * - 구독 상태 + 사용량 + 리셋일 관리
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organization_subscription")
public class OrganizationSubscription extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_subscription_id")
    private Long id;

    /** 기관 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** 구독 플랜 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** 구독 시작일 / 종료일 / 갱신일 */
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private LocalDateTime subscriptionRenewalDate;

    /** ✅ 구독 리셋일 (플랜 주기에 따라 계산됨) */
    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate;

    /** ✅ 사용량 관련 필드 */
    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer remainingResponses = 0;

    @Column(nullable = false)
    private Double usageRate = 0.0;

    /** 구독 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    /** 자동 갱신 여부 */
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    /* ---------- 비즈니스 로직 ---------- */

    /** 구독 초기화 (기관 신규 등록 시) */
    public void initializeSubscription() {
        LocalDateTime now = LocalDateTime.now();

        // ✅ 구독 주기 계산 (기본 monthly)
        LocalDateTime nextCycleDate;
        if (subscriptionPlan != null && subscriptionPlan.isYearly()) {
            nextCycleDate = now.plusYears(1);
        } else {
            nextCycleDate = now.plusMonths(1);
        }

        // ✅ 안전한 쿼터 계산
        int quota = safeQuota();

        // ✅ 모든 필드 명확히 초기화
        this.subscriptionStartDate = now;
        this.subscriptionEndDate = nextCycleDate;
        this.subscriptionRenewalDate = nextCycleDate;

        // ✅ usageResetDate도 반드시 세팅 (💡 NPE 원인 제거)
        this.usageResetDate = nextCycleDate;

        // ✅ 사용량 초기화
        this.successCount = 0;
        this.remainingResponses = quota;
        this.usageRate = 0.0;

        // ✅ 상태 및 자동갱신 활성화
        this.status = SubscriptionStatus.ACTIVE;
        this.autoRenew = true;
    }

    /** 분석 성공 시 사용량 업데이트 */
    public void increaseSuccessCount() {
        this.successCount++;
        if (this.remainingResponses > 0) {
            this.remainingResponses--; // ✅ 남은 성공량 감소
        }
        updateUsageRate();
    }

    /** 사용률 계산 */
    public void updateUsageRate() {
        int quota = safeQuota();
        if (quota > 0) {
            int used = Math.min(successCount, quota);
            this.usageRate = (used * 100.0) / quota;
        } else {
            this.usageRate = 0.0;
        }
    }

    /** 남은 응답이 0 이하인지 */
    public boolean isOverused() {
        return remainingResponses <= 0;
    }

    /** NPE 방지용 */
    private int safeQuota() {
        return (subscriptionPlan != null && subscriptionPlan.getMaxSuccessResponses() != null)
                ? subscriptionPlan.getMaxSuccessResponses()
                : 0;
    }
}