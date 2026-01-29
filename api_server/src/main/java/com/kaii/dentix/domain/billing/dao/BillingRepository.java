package com.kaii.dentix.domain.billing.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {
    //특정 기관의 전체 빌링 내역 조회 (페이징)
    Page<Billing> findByOrganization(Organization organization, Pageable pageable);

    //특정 기관의 상태별 빌링 내역 조회 (페이징)
    Page<Billing> findByOrganizationAndBillingStatus(Organization organization, BillingStatus billingStatus, Pageable pageable);

    boolean existsByOrganizationAndBillingTypeAndBillingStatus(
            Organization organization,
            BillingType billingType,
            BillingStatus billingStatus
    );

    List<Billing> findAllByBillingStatus(BillingStatus status);

    List<Billing> findAllByOrganization_OrganizationIdOrderByBilledAtDesc(Long organizationId);

    List<Billing> findAllByOrganizationOrderByCreatedDesc(Organization organization);

    boolean existsByOrganizationAndPeriodStart(
            Organization organization,
            LocalDateTime periodStart
    );

    List<Billing> findAllByOrganization(Organization organization);

    List<Billing> findAllByOrganization_OrganizationId(Long organizationId);

    List<Billing> findByOrganizationAndBillingTypeIn(
            Organization organization,
            List<BillingType> billingTypes
    );

    List<Billing> findByOrganizationAndBillingTypeAndBilledAtBetween(
            Organization organization,
            BillingType billingType,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Billing> findByOrganizationAndBillingTypeInOrderByBilledAtDesc(
            Organization organization,
            List<BillingType> billingTypes
    );

    @Query("""
    select b
    from Billing b
    where b.organization.organizationId = :orgId
      and b.billingType = :type
      and b.billedAt >= :start
      and (:end is null or b.billedAt <= :end)
""")
    List<Billing> findOveruseBillingsInPeriod(
            @Param("orgId") Long orgId,
            @Param("type") BillingType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    SELECT b
    FROM Billing b
    LEFT JOIN FETCH b.subscriptionPlan
    WHERE b.organization.organizationId = :orgId
      AND (:statusEnum IS NULL OR b.billingStatus = :statusEnum)
""")
    Page<Billing> findByOrganizationWithStatusAndPlan(
            @Param("orgId") Long orgId,
            @Param("statusEnum") BillingStatus statusEnum,
            Pageable pageable
    );
}