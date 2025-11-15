package com.kaii.dentix.domain.billing.domain;


import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "billing_status_history")
public class BillingStatusHistory extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billingStatusHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingStatus newStatus;

    // 누가 변경했는지 (슈퍼관리자 아이디)
    @Column(length = 100)
    private String changedBy;

    // 필요하면 간단 메모
    @Column(length = 255)
    private String memo;

    public static BillingStatusHistory of(Billing billing,
                                          BillingStatus oldStatus,
                                          BillingStatus newStatus,
                                          String changedBy,
                                          String memo) {
        return BillingStatusHistory.builder()
                .billing(billing)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .memo(memo)
                .build();
    }
}