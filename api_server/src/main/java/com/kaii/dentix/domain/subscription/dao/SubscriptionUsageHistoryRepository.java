package com.kaii.dentix.domain.subscription.dao;


//import com.kaii.dentix.domain.subscription.domain.SubscriptionUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

/**
 * ✅ 기관별 월별 사용량 조회용 Repository
 */
//@Repository
//public interface SubscriptionUsageHistoryRepository extends JpaRepository<SubscriptionUsageHistory, Long> {
//
//    /**
//     * ✅ 기관 ID와 사용월 기준으로 사용 이력 조회
//     * - 사용월(usageMonth)은 YearMonthConverter로 DB에 문자열로 저장됨 ("YYYY-MM")
//     */
//    @Query("SELECT u FROM SubscriptionUsageHistory u " +
//            "WHERE u.organization.organizationId = :orgId " +
//            "AND u.usageMonth = :usageMonth")
//    Optional<SubscriptionUsageHistory> findByOrganizationIdAndUsageMonth(
//            @Param("orgId") Long orgId,
//            @Param("usageMonth") YearMonth usageMonth
//    );
//}