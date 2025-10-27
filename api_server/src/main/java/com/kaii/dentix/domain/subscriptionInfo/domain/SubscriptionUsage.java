package com.kaii.dentix.domain.subscriptionInfo.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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