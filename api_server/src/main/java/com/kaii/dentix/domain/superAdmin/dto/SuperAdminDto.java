package com.kaii.dentix.domain.superAdmin.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.BillingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class SuperAdminDto {

    // =================================================================
    // 1. 기관 목록 조회 (OrganizationListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class OrganizationListResponse {
        private Long organizationId;
        private String organizationName;
        private String organizationPhoneNumber;
        private String subscriptionPlanName;
        private Long price;
        private String startDate;
        private String endDate;
        private Boolean active;

        public static OrganizationListResponse fromEntity(Organization o) {
            return OrganizationListResponse.builder()
                    .organizationId(o.getOrganizationId())
                    .organizationName(o.getOrganizationName())
                    .organizationPhoneNumber(o.getOrganizationPhoneNumber())
                    .subscriptionPlanName(o.getSubscriptionPlan() != null ? o.getSubscriptionPlan().getPlanName().name() : null)
                    .price(o.getSubscriptionPlan() != null ? o.getSubscriptionPlan().getPrice() : null)
                    .startDate(o.getSubscriptionStartDate() != null ? o.getSubscriptionStartDate().toString() : null)
                    .endDate(o.getSubscriptionEndDate() != null ? o.getSubscriptionEndDate().toString() : null)
                    .active(o.getActive())
                    .build();
        }
    }

    // =================================================================
    // 2. 기관 상세 조회 (OrganizationDetailResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class OrganizationDetailResponse {
        private Long organizationId;
        private String organizationName;
        private String organizationEmail;
        private String organizationPhoneNumber;
        private String subscriptionPlanName;
        private Long price;
        private String startDate;
        private String endDate;
        private Boolean active;

        public static OrganizationDetailResponse fromEntity(Organization o) {
            return OrganizationDetailResponse.builder()
                    .organizationId(o.getOrganizationId())
                    .organizationName(o.getOrganizationName())
                    .organizationEmail(o.getOrganizationEmail())
                    .organizationPhoneNumber(o.getOrganizationPhoneNumber())
                    .subscriptionPlanName(o.getSubscriptionPlan() != null ? o.getSubscriptionPlan().getPlanName().name() : null)
                    .price(o.getSubscriptionPlan() != null ? o.getSubscriptionPlan().getPrice() : null)
                    .startDate(o.getSubscriptionStartDate() != null ? o.getSubscriptionStartDate().toString() : null)
                    .endDate(o.getSubscriptionEndDate() != null ? o.getSubscriptionEndDate().toString() : null)
                    .active(o.getActive())
                    .build();
        }
    }

    // =================================================================
    // 3. 현재 구독 정보 (CurrentSubscriptionResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class CurrentSubscriptionResponse {
        private Long organizationId;
        private String organizationName;
        private Long subscriptionPlanId;
        private String subscriptionPlanName;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private boolean active;
    }

    // =================================================================
    // 4. 결제 내역 리스트 (BillingListResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class BillingListResponse {
        private Long organizationId;
        private String organizationName;
        private List<BillingSummary> billings;

        @Getter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class BillingSummary {
            private Long billingId;
            private BillingType billingType;
            private Long amount;
            private LocalDateTime billedAt;
            private LocalDateTime periodStart;
            private LocalDateTime periodEnd;
            private String description;
        }

        public static BillingListResponse from(Organization org, List<Billing> list) {
            return BillingListResponse.builder()
                    .organizationId(org.getOrganizationId())
                    .organizationName(org.getOrganizationName())
                    .billings(
                            list.stream().map(b -> BillingSummary.builder()
                                    .billingId(b.getId())
                                    .billingType(b.getBillingType())
                                    .amount(b.getAmount())
                                    .billedAt(b.getBilledAt())
                                    .periodStart(b.getPeriodStart())
                                    .periodEnd(b.getPeriodEnd())
                                    .description(b.getDescription())
                                    .build()
                            ).toList()
                    )
                    .build();
        }
    }

    // =================================================================
    // 5. 사용자 통계 (Statistics) - 기존 SuperAdminStatisticDto 통합
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TotalUserStats {
        private Long totalUsers;
        private Long maleUsers;
        private Long femaleUsers;
        private Long newUsers7Days;
        private List<OrgUserStats> organizationStats;
    }

    @Getter @Builder
    @NoArgsConstructor
    public static class OrgUserStats {
        private Long organizationId;
        private String organizationName;
        private Long totalUsers;
        private Long maleUsers;
        private Long femaleUsers;
        private Long newUsers;

        public OrgUserStats(Long organizationId, String organizationName, Long totalUsers, Long maleUsers, Long femaleUsers, Long newUsers) {
            this.organizationId = organizationId;
            this.organizationName = organizationName;
            this.totalUsers = totalUsers;
            this.maleUsers = maleUsers;
            this.femaleUsers = femaleUsers;
            this.newUsers = newUsers;
        }
    }
}