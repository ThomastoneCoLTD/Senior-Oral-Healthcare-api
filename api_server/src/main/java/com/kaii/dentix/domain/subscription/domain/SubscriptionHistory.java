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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column(length = 100)
    private String reason;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer overuseCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    public void increaseUsage() {
        if (successCount < subscriptionPlan.getMaxSuccessResponses()) {
            successCount++;
        } else {
            overuseCount++;
        }
    }

    public void closeHistory(String reason) {
        this.active = false;
        this.endDate = LocalDateTime.now();
        this.reason = reason;
    }

    @PrePersist
    public void prePersist() {
        if (startDate == null) startDate = LocalDateTime.now();
        if (successCount == null) successCount = 0;
        if (overuseCount == null) overuseCount = 0;
    }
}
