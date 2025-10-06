package com.kaii.dentix.domain.subscriptionPlan.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subscriptionHistory")
public class SubscriptionHistory extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 구독 변경 대상 기관
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationId", nullable = false)
    private Organization organization;

    // ✅ 구독한 플랜
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate; // 구독 시작일

    @Column(name = "end_date")
    private LocalDateTime endDate;   // 구독 종료일 (null이면 현재 플랜 유지 중)
}
