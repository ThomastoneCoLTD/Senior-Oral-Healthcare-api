package com.kaii.dentix.domain.organizationSubscriptionHistory.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
public interface OrganizationSubscriptionHistoryRepository extends JpaRepository<OrganizationSubscriptionHistory, Long> {
    /** ✅ 기관별 구독 이력 (최신순) */
   List<OrganizationSubscriptionHistory> findAllByOrganizationOrderByStartDateDesc(Organization organization);

    @Query("""
    SELECT h
    FROM OrganizationSubscriptionHistory h
    JOIN FETCH h.organization o
    JOIN FETCH h.subscriptionPlan p
    WHERE o.organizationId = :organizationId
    ORDER BY h.startDate DESC
""")
    List<OrganizationSubscriptionHistory> findAllByOrgIdWithFetch(Long organizationId);
}
