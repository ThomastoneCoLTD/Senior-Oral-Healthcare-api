package com.kaii.dentix.domain.organization.domain;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organization")
@SQLDelete(sql = "UPDATE organization SET deleted = NOW() WHERE organization_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Organization extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long organizationId;

    @Column(length = 100, nullable = false)
    private String organizationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(length = 20, nullable = false, unique = true)
    private String organizationPhoneNumber;

    @Column(name = "success_count", nullable = false)
    private Integer successCount; // 성공 응답 수

    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate; // 리셋 예정일

    // ✅ 새로 추가 (사용률)
    @Column(name = "usage_rate", nullable = false)
    private Double usageRate = 0.0;

    // ✅ 1:N 관계 (Organization → Admin)
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Admin> admins = new ArrayList<>();

    @Column(name = "deleted")
    private LocalDateTime deleted;

    //기관 정보 수정
    public void updateOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void updateSubscriptionPlan(SubscriptionPlan plan) {
        this.subscriptionPlan = plan;
    }

    //기관 삭제
    public void deleteOrganization() {
        this.deleted = LocalDateTime.now();
    }

    public void updateSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public void updateUsageResetDate(LocalDateTime usageResetDate) {
        this.usageResetDate = usageResetDate;
    }

    public void updateSubscriptionStartDate(LocalDateTime date) {
        this.subscriptionStartDate = date;
    }

    // ✅ 새로 추가: 사용률 갱신 메서드
    public void updateUsageRate(Double usageRate) {
        this.usageRate = usageRate;
    }

    @PrePersist
    public void prePersist() {
        if (this.successCount == null) {
            this.successCount = 0;
        }
        if (this.usageRate == null) {
            this.usageRate = 0.0;
        }
    }
}
