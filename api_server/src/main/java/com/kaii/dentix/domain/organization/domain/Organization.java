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
 * 기관 (Organization)
 * 어떤 구독상품을 사용하는지, 사용량/리셋/초과 관리
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

    @Column(length = 100, unique = true)
    private String organizationEmail;

    @Column(length = 20, nullable = false, unique = true)
    private String organizationPhoneNumber;

    @Column(length = 200)
    private String organizationAddress;

    @Column(length = 500)
    private String description;

    @Column
    private Boolean active = true;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "deleted")
    private LocalDateTime deleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlan subscriptionPlan;

    /** 기관 관리자 (Admin) 리스트 */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Admin> admins = new ArrayList<>();

    /** 현재 구독 상태 (1:1 매핑) */
    @OneToOne(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrganizationSubscription organizationSubscription;

    /** 편의 메서드 */
    public void deactivate() {
        this.active = false;
        this.deleted = LocalDateTime.now();
    }
//    /** 구독이력 (선택사항) */
//    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<SubscriptionHistory> subscriptionHistories = new ArrayList<>();

    @Transient
    public SubscriptionPlan getCurrentSubscriptionPlan() {
        return (this.organizationSubscription != null)
                ? this.organizationSubscription.getSubscriptionPlan()
                : null;
    }
}