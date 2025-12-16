package com.kaii.dentix.domain.organizationSubscriptionHistory.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrganizationSubscriptionHistoryRepository extends JpaRepository<OrganizationSubscriptionHistory, Long> {
    /** ✅ 기관별 구독 이력 (최신순) */
   List<OrganizationSubscriptionHistory> findAllByOrganizationOrderByStartDateDesc(Organization organization);

    @Query("""
        select h
        from OrganizationSubscriptionHistory h
        join fetch h.subscriptionPlan
        where h.organization.organizationId = :orgId
        order by h.startDate desc
        """)
    List<OrganizationSubscriptionHistory> findAllByOrgIdWithFetch(@Param("orgId") Long orgId);
    List<OrganizationSubscriptionHistory>
    findAllByOrganization_OrganizationIdOrderByStartDateDesc(Long organizationId);

    Optional<OrganizationSubscriptionHistory>
    findTopByOrganization_OrganizationIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanOrderByStartDateDesc(
            Long organizationId,
            SubscriptionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

}
