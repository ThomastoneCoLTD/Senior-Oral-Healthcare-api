package com.kaii.dentix.domain.organizationSubscriptionHistory.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface OrganizationSubscriptionHistoryRepository extends JpaRepository<OrganizationSubscriptionHistory, Long> {
    /** ✅ 기관별 구독 이력 (최신순) */
   List<OrganizationSubscriptionHistory> findAllByOrganizationOrderByStartDateDesc(Organization organization);

    @Query("""
        SELECT osh
        FROM OrganizationSubscriptionHistory osh
        JOIN FETCH osh.subscriptionPlan sp
        JOIN FETCH osh.organization o
        WHERE o.organizationId = :orgId
        ORDER BY osh.startDate DESC
    """)
    List<OrganizationSubscriptionHistory> findAllByOrgIdWithFetch(@Param("orgId") Long orgId);

    List<OrganizationSubscriptionHistory>
    findAllByOrganization_OrganizationIdOrderByStartDateDesc(Long organizationId);
}
