package com.kaii.dentix.domain.subscription.dao;


import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    /** 특정 기관의 구독 이력 전체 */
    List<SubscriptionHistory> findAllByOrganization_OrganizationId(Long organizationId);
    /** 현재 활성화된 (endDate가 null인) 구독 이력 */
    Optional<SubscriptionHistory> findByOrganization_OrganizationIdAndEndDateIsNull(Long organizationId);
    /** ✅ 특정 기관의 구독 이력 전체 (최신순) */
    List<SubscriptionHistory> findAllByOrganization_OrganizationIdOrderByStartDateDesc(Long organizationId);
    Optional<SubscriptionHistory> findTopByOrganizationOrderByStartDateDesc(Organization organization);
    Optional<SubscriptionHistory> findByOrganizationAndActiveTrue(Organization organization);

    @Query("""
    SELECT h
    FROM SubscriptionHistory h
    JOIN FETCH h.organization o
    JOIN FETCH h.subscriptionPlan p
    WHERE o.organizationId = :organizationId
    ORDER BY h.startDate DESC
""")
    List<SubscriptionHistory> findAllWithFetchByOrganizationId(@Param("organizationId") Long organizationId);


}
