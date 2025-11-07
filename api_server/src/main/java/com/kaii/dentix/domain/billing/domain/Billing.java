package com.kaii.dentix.domain.billing.domain;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Billing extends TimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private int amount; // 청구 금액 (원)
    private String description; // 예: “AI 분석 초과 사용 요금” / “Small 월 구독 요금”

    @Enumerated(EnumType.STRING)
    private BillingStatus status; // UNPAID / PAID

//    private LocalDateTime createdAt = LocalDateTime.now();
}