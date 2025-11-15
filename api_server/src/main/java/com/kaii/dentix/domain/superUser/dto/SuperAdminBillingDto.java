package com.kaii.dentix.domain.superUser.dto;

import com.kaii.dentix.domain.billing.domain.Billing;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SuperAdminBillingDto {

    private Long billingId;
    private Long organizationId;
    private String organizationName;
    private Long subscriptionPlanId;
    private String subscriptionPlanName;
    private String billingType;
    private String billingStatus;
    private Double amount;
    private LocalDateTime billedAt;
    private LocalDateTime paidAt;
    private String paymentRef;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String description;

    public SuperAdminBillingDto(Billing b) {
        this.billingId = b.getId();

        // organization
        this.organizationId = b.getOrganization().getOrganizationId();
        this.organizationName = b.getOrganization().getOrganizationName();

        // subscription plan
        this.subscriptionPlanId = b.getSubscriptionPlan().getId();
        this.subscriptionPlanName = b.getSubscriptionPlan().getPlanName().name();

        this.billingType = b.getBillingType().name();
        this.billingStatus = b.getBillingStatus().name();
        this.amount = b.getAmount();
        this.billedAt = b.getBilledAt();
        this.paidAt = b.getPaidAt();
        this.paymentRef = b.getPaymentRef();
        this.periodStart = b.getPeriodStart();
        this.periodEnd = b.getPeriodEnd();
        this.description = b.getDescription();
    }
}