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

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "subscription_renewal_date")
    private LocalDateTime subscriptionRenewalDate;

    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate;

    /** 사용량 관련 필드 */
    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "remaining_responses")
    private Integer remainingResponses;

    @Column(name = "usage_rate")
    private Double usageRate;

    /** 구독 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SubscriptionStatus status;

    /** 자동 갱신 여부 */
    @Column(name = "auto_renew")
    private Boolean autoRenew;

    @Column(name = "active")
    private Boolean active;
    /* ---------- 비즈니스 로직 ---------- */

    /** ======================================
     *  안전 기본값 초기화 (NPE 방지)
     * ====================================== */
    private void ensureInitialized() {
        if (this.successCount == null) {
            this.successCount = 0;
        }
        if (this.remainingResponses == null) {
            this.remainingResponses = safeQuota();
        }
        if (this.usageRate == null) {
            this.usageRate = 0.0;
        }
        if (this.autoRenew == null) {
            this.autoRenew = true;
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    /** ======================================
     *  안전한 쿼터 계산
     * ====================================== */
    private int safeQuota() {
        return (subscriptionPlan != null && subscriptionPlan.getMaxSuccessResponses() != null)
                ? subscriptionPlan.getMaxSuccessResponses()
                : 0;
    }

    /** ======================================
     *  🔥 구독 초기화 (함수명 유지)
     * ====================================== */
    public void initializeSubscription() {
        ensureInitialized();

        LocalDateTime now = LocalDateTime.now();

        // 🔥 결제 주기 (플랜 연간/월간 구분)
        LocalDateTime nextCycleDate;
        if (subscriptionPlan != null && subscriptionPlan.isYearly()) {
            nextCycleDate = now.plusYears(1);
        } else {
            nextCycleDate = now.plusMonths(1);
        }

        // 🔥 사용량 리셋일 (무조건 월간)
        LocalDateTime nextUsageReset = now.plusMonths(1);

        // 🔥 쿼터 계산
        int quota = safeQuota();

        // 👇 구독 정보 세팅
        this.subscriptionStartDate = now;
        this.subscriptionEndDate = nextCycleDate;
        this.subscriptionRenewalDate = nextCycleDate;

        this.usageResetDate = nextUsageReset;

        this.successCount = 0;
        this.remainingResponses = quota;
        this.usageRate = 0.0;

        this.status = SubscriptionStatus.ACTIVE;
        this.autoRenew = true;
        this.active = true;
    }

    /** ======================================
     *  분석 성공 → 사용량 증가
     * ====================================== */
    public void increaseSuccessCount() {
        ensureInitialized();

        this.successCount++;

        if (this.remainingResponses != null && this.remainingResponses > 0) {
            this.remainingResponses = this.remainingResponses - 1;
        }

        updateUsageRate();
    }

    /** ======================================
     *  사용률 계산
     * ====================================== */
    public void updateUsageRate() {
        ensureInitialized();

        int quota = safeQuota();
        if (quota > 0) {
            int used = Math.min(this.successCount, quota);
            this.usageRate = (used * 100.0) / quota;
        } else {
            this.usageRate = 0.0;
        }
    }

    /** ======================================
     *  초과 사용 여부
     * ====================================== */
    public boolean isOverused() {
        ensureInitialized();
        return this.remainingResponses != null && this.remainingResponses <= 0;
    }

}