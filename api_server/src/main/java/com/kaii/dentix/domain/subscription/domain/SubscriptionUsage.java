package com.kaii.dentix.domain.subscription.domain;
import com.kaii.dentix.domain.organization.domain.Organization;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ✅ 현재 활성 구독 주기 (Subscription Usage)
 * - 한 기관당 active=true 인 주기는 하나만 존재
 * - 매월/매년 종료 시 히스토리로 이동
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription_usage")
public class SubscriptionUsage extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionUsageId;

    /** ✅ 기관 (ManyToOne) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** ✅ 구독 플랜 (ManyToOne) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** ✅ 사용 시작일 */
    @Column(nullable = false)
    private LocalDateTime periodStart;

    /** ✅ 사용 종료일 (다음 결제/갱신일) */
    @Column(nullable = false)
    private LocalDateTime periodEnd;

    /** ✅ 사용 횟수 */
    @Column(nullable = false)
    private Integer successCount = 0;

    /** ✅ 초과 사용 횟수 */
    @Column(nullable = false)
    private Integer overuseCount = 0;

    /** ✅ 활성 상태 (현재 주기만 true) */
    @Column(nullable = false)
    private Boolean active = true;

    /** ✅ 남은 사용 가능 횟수 (optional) */
    @Column(nullable = false)
    private Integer remainingCount = 0;

    // -------------------------------
    // ✅ 비즈니스 로직
    // -------------------------------

    /** 사용 1회 증가 */
    public void increaseUsage() {
        this.successCount++;
        if (this.remainingCount > 0) {
            this.remainingCount--;
        } else {
            this.overuseCount++;
        }
    }

    /** 사용량 리셋 (주기 갱신 시 호출) */
    public void resetUsage() {
        this.successCount = 0;
        this.overuseCount = 0;
        this.remainingCount = subscriptionPlan.getMaxSuccessResponses();
        this.active = true;
        this.periodStart = LocalDateTime.now();
        this.periodEnd = calculateNextPeriodEnd();
    }

    /** 주기 계산 (플랜 타입에 따라 종료일 계산) */
    private LocalDateTime calculateNextPeriodEnd() {
        String cycle = subscriptionPlan.getPlanCycle();
        if ("yearly".equalsIgnoreCase(cycle)) {
            return LocalDateTime.now().plusYears(1);
        } else {
            return LocalDateTime.now().plusMonths(1);
        }
    }

    /** 비활성화 (새 주기 시작 시 호출) */
    public void deactivate() {
        this.active = false;
    }

    /** 남은 횟수 초기 세팅 */
    @PrePersist
    public void prePersist() {
        if (this.remainingCount == null || this.remainingCount == 0) {
            this.remainingCount = subscriptionPlan != null
                    ? subscriptionPlan.getMaxSuccessResponses()
                    : 0;
        }
        if (this.periodStart == null) this.periodStart = LocalDateTime.now();
        if (this.periodEnd == null) this.periodEnd = calculateNextPeriodEnd();
        if (this.active == null) this.active = true;
        if (this.successCount == null) this.successCount = 0;
        if (this.overuseCount == null) this.overuseCount = 0;
    }
}