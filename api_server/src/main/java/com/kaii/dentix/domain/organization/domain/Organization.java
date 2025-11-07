package com.kaii.dentix.domain.organization.domain;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ 기관 (Organization)
 * - 어떤 구독상품을 사용하는지, 사용량/리셋/초과 관리
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organization")
@SQLDelete(sql = "UPDATE organization SET deleted = NOW() WHERE organization_id = ?")
@Where(clause = "deleted IS NULL")
public class Organization extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(length = 100, nullable = false)
    private String organizationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate; // ✅ 월/년 단위로 종료일 관리

    @Column(length = 20, nullable = false, unique = true)
    private String organizationPhoneNumber;

    /** ✅ 사용량 관리 */
    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0; // 누적 성공 응답 수

    @Column(name = "remaining_responses", nullable = false)
    private Integer remainingResponses = 0; // 남은 사용 가능 횟수

    @Column(name = "usage_reset_date")
    private LocalDateTime usageResetDate; // 리셋 예정일

    @Column(name = "usage_rate", nullable = false)
    private Double usageRate = 0.0; // 사용률

    /** ✅ 초과 사용 누적 (Billing 생성용) */
    @Column(name = "overuse_count", nullable = false)
    private Integer overuseCount = 0;

    /** ✅ 소속 관리자 목록 */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Admin> admins = new ArrayList<>();

    @Column(name = "deleted")
    private LocalDateTime deleted;

    /** -------------------- 비즈니스 로직 -------------------- */

    // 기관명 수정
    public void updateOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    // 플랜 변경 (플랜 변경 시 사용량 리셋)
    public void updateSubscriptionPlan(SubscriptionPlan plan) {
        this.subscriptionPlan = plan;
        this.remainingResponses = plan.getMaxSuccessResponses();
        this.usageRate = 0.0;
    }

    // 사용 횟수 증가 (AI 분석 성공 시 호출)
    public void increaseUsage() {
        this.successCount++;
        if (remainingResponses > 0) {
            remainingResponses--;
        } else {
            overuseCount++;
        }
        updateUsageRate();
    }

    // 사용률 계산
    public void updateUsageRate() {
        int total = subscriptionPlan.getMaxSuccessResponses();
        if (total > 0) {
            this.usageRate = (double) successCount / total * 100.0;
        } else {
            this.usageRate = 0.0;
        }
    }

    // 남은 사용량 리셋 (매월 1일)
    public void resetUsage() {
        this.remainingResponses = subscriptionPlan.getMaxSuccessResponses();
        this.successCount = 0;
        this.overuseCount = 0;
        this.usageRate = 0.0;
        this.usageResetDate = LocalDateTime.now().plusMonths(1);
    }

    // 기관 삭제 (soft delete)
    public void deleteOrganization() {
        this.deleted = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.successCount == null) this.successCount = 0;
        if (this.remainingResponses == null)
            this.remainingResponses = subscriptionPlan != null ? subscriptionPlan.getMaxSuccessResponses() : 0;
        if (this.overuseCount == null) this.overuseCount = 0;
        if (this.usageRate == null) this.usageRate = 0.0;
    }
}