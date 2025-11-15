package com.kaii.dentix.domain.billing.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
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

/**
 * ✅ BillingRepository
 * - 기관별 청구 내역 조회 / 합계 / 상태 변경용 Repository
 */
@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {
    List<Billing> findAllByOrganization_OrganizationIdOrderByCreatedDesc(Long organizationId);


    List<Billing> findAllByBillingStatus(BillingStatus status);
    /** ✅ 기관별 청구 내역 (최신순) */
    List<Billing> findAllByOrganization_OrganizationIdOrderByBilledAtDesc(Long organizationId);

    boolean existsByOrganizationAndBillingTypeAndBillingStatusAndCreatedAfterAndCreatedBefore(
            Organization organization,
            BillingType billingType,
            BillingStatus billingStatus,
            Date start,
            Date end
    );
    @Query("""
    SELECT b
    FROM Billing b
    JOIN FETCH b.organization o
    JOIN FETCH b.subscriptionPlan sp
    WHERE o.organizationId = :organizationId
    ORDER BY b.created DESC
""")
    List<Billing> findAllWithOrganizationAndPlan(Long organizationId);
    @Query("""
    SELECT o FROM Organization o
    JOIN FETCH o.organizationSubscription os
    JOIN FETCH os.subscriptionPlan
    WHERE o.organizationId = :id
""")
    Optional<Organization> findByIdWithPlan(@Param("id") Long id);

    /** ✅ 특정 기관의 Billing 목록을 생성일 기준 내림차순으로 조회 */
    List<Billing> findAllByOrganizationOrderByCreatedDesc(Organization organization);
    /**
     * ✅ 특정 기관에 동일한 periodStart(시작일)를 가진 Billing이 이미 존재하는지 확인
     * → BillingScheduler 중복 생성 방지용
     */
    boolean existsByOrganizationAndPeriodStart(Organization organization, LocalDateTime periodStart);

    List<Billing> findAllByOrganizationOrderByBilledAtDesc(Organization organization);

    List<Billing> findAllByOrganization_OrganizationId(Long organizationId);

    /**
     * ✅ 특정 기관의 모든 Billing 내역 조회
     * (엑셀 Export 용)
     */
    List<Billing> findAllByOrganization(Organization organization);
//    List<Billing> findAllByOrganizationOrderByCreatedDesc(Organization organization);

    @Query("""
SELECT b
FROM Billing b
LEFT JOIN FETCH b.subscriptionPlan
WHERE b.organization.organizationId = :orgId
AND (:status = 'ALL' OR b.billingStatus = :statusEnum)
""")
    Page<Billing> findByOrganizationWithStatusAndPlan(
            @Param("orgId") Long orgId,
            @Param("status") String status,
            @Param("statusEnum") BillingStatus statusEnum,
            Pageable pageable
    );
}
