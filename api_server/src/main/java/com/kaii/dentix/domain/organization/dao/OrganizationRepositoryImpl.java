package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.QOrganization;
import com.kaii.dentix.domain.subscriptionPlan.domain.QSubscriptionPlan;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrganizationRepositoryImpl implements OrganizationRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdminOrganizationUsageResponse> findAllOrganizationUsage() {
        QOrganization org = QOrganization.organization;
        QSubscriptionPlan plan = QSubscriptionPlan.subscriptionPlan;

        return queryFactory
                .select(
                        Projections.constructor(
                                AdminOrganizationUsageResponse.class,
                                org.organizationId,
                                org.organizationName,
                                plan.planName,
                                plan.maxSuccessResponses,
                                org.successCount,
                                plan.maxSuccessResponses.subtract(org.successCount).as("remainingCount"),
                                org.successCount
                                        .castToNum(Double.class)
                                        .divide(plan.maxSuccessResponses.castToNum(Double.class))
                                        .multiply(100.0)
                                        .as("usageRate")
                        )
                )
                .from(org)
                .join(org.subscriptionPlan, plan)
                .orderBy(org.organizationId.asc())
                .fetch();
    }
}
