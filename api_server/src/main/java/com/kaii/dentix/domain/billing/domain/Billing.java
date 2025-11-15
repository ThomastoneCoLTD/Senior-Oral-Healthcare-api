package com.kaii.dentix.domain.billing.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * ✅ 결제 청구 내역 (Billing)
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "billing")
public class Billing extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_id")
    private Long id;

    /** 기관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** 구독 플랜 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** 결제 유형 (정기, 초과 등) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingType billingType;

    /** 결제 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingStatus billingStatus;

    /** 결제 금액 */
    @Column(nullable = false)
    private Double amount;

    /** 청구일 */
    @Column(nullable = true)
    private LocalDateTime billedAt;

    /** 결제 완료일 */
    private LocalDateTime paidAt;

    /** 결제 트랜잭션 ID (PG사 reference 등) */
    @Column(length = 100)
    private String paymentRef;

    /** 청구 주기 시작일 ~ 종료일 */
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    /** 비고 */
    private String description;

    /* ---------- 비즈니스 메서드 ---------- */

    public void markPaid(String paymentRef) {
        this.billingStatus = BillingStatus.PAID;
        this.paymentRef = paymentRef;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.billingStatus = BillingStatus.FAILED;
    }
}