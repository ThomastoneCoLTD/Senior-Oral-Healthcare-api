package com.kaii.dentix.domain.organizationSubscriptionHistory.dao;

import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface OrganizationSubscriptionHistoryRepository
        extends JpaRepository<OrganizationSubscriptionHistory, Long> {

 @Query("""
        select h
        from OrganizationSubscriptionHistory h
        join fetch h.subscriptionPlan
        where h.organization.organizationId = :orgId
        order by h.startDate desc
    """)
 List<OrganizationSubscriptionHistory> findAllByOrgIdWithFetch(
         @Param("orgId") Long orgId
 );

 // 현재 활성 구독 (유일)
 Optional<OrganizationSubscriptionHistory>
 findByOrganization_OrganizationIdAndEndDateIsNull(Long organizationId);

 // 이력 조회
 List<OrganizationSubscriptionHistory>
 findAllByOrganization_OrganizationIdOrderByStartDateDesc(Long organizationId);
}
