package com.kaii.dentix.domain.billing.dao;

import com.kaii.dentix.domain.type.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ✅ BillingRepository
 * - 기관별 청구 내역 조회 / 합계 / 상태 변경용 Repository
 */
@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    /**
     * ✅ 특정 기관의 미정산(UNPAID) Billing 리스트 조회
     */
    @Query("SELECT b FROM Billing b " +
            "WHERE b.organization.organizationId = :orgId " +
            "AND b.status = :status " +
            "ORDER BY b.createdAt DESC")
    List<Billing> findByOrganizationIdAndStatus(
            @Param("orgId") Long orgId,
            @Param("status") BillingStatus status
    );

    /**
     * ✅ 특정 기관의 미정산 금액 총합 조회
     */
    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Billing b " +
            "WHERE b.organization.organizationId = :orgId " +
            "AND b.status = 'UNPAID'")
    int sumUnpaidAmount(@Param("orgId") Long orgId);

    /**
     * ✅ 가장 최근 Billing 1건 조회
     */
    @Query("SELECT b FROM Billing b " +
            "WHERE b.organization.organizationId = :orgId " +
            "ORDER BY b.createdAt DESC LIMIT 1")
    Optional<Billing> findLatestByOrganizationId(@Param("orgId") Long orgId);

    /**
     * ✅ 모든 미정산 Billing 조회 (Scheduler나 Admin 대시보드용)
     */
    @Query("SELECT b FROM Billing b WHERE b.status = 'UNPAID' ORDER BY b.createdAt DESC")
    List<Billing> findAllUnpaidBillings();
}
