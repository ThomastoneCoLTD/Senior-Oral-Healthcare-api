package com.kaii.dentix.domain.subscription.domain;

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
@Table(name = "subscription_history")
public class SubscriptionHistory extends TimeEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_history_id")
    private Long id;

    /** 기관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** 구독 플랜 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /** 구독 시작일 */
    @Column(nullable = false)
    private LocalDateTime startDate;

    /** 구독 종료일 */
    @Column
    private LocalDateTime endDate;

    /** 변경 사유 (optional: 예를 들어 “플랜 업그레이드”, “자동 갱신”) */
    @Column(length = 100)
    private String reason;

    /** 편의 메서드 */
    public void closeHistory(String reason) {
        this.endDate = LocalDateTime.now();
        this.reason = reason;
    }
}
