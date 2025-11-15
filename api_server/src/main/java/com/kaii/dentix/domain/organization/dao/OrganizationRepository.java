package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>,OrganizationRepositoryCustom {
//    Optional<Organization> findByOrganizationId(Long organizationId);
//
//    @Modifying
//    @Transactional
//    @Query("UPDATE Organization o SET o.successCount = o.successCount + 1 WHERE o.organizationId = :orgId")
//    void increaseSuccessCount(@Param("orgId") Long orgId);

    Optional<Organization> findByOrganizationPhoneNumber(String organizationPhoneNumber);

    @Query("""
        SELECT o
        FROM Organization o
        JOIN FETCH o.organizationSubscription os
        JOIN FETCH os.subscriptionPlan sp
        WHERE o.organizationPhoneNumber = :phone
        """)
    Optional<Organization> findByPhoneWithPlan(@Param("phone") String phone);
//
//    @Modifying
//    @Query("UPDATE Organization o SET o.successCount = :count WHERE o.organizationId = :orgId")
//    void updateSuccessCount(@Param("orgId") Long orgId, @Param("count") long count);
//
//    @Query("SELECT o FROM Organization o JOIN FETCH o.subscriptionPlan")
//    List<Organization> findAllWithSubscription();
//
//    /**
//     * ✅ 기관 ID로 Organization + SubscriptionPlan을 함께 조회 (Fetch Join)
//     * - Lazy 로딩 방지
//     * - Soft Delete 필터 적용 (deleted IS NULL)
//     */
//    @Query("SELECT o FROM Organization o " +
//            "JOIN FETCH o.subscriptionPlan p " +
//            "WHERE o.organizationId = :orgId " +
//            "AND o.deleted IS NULL")
//    Optional<Organization> findWithSubscriptionPlanById(@Param("orgId") Long orgId);

    boolean existsByOrganizationName(String organizationName);
    boolean existsByOrganizationPhoneNumber(String organizationPhoneNumber);
    boolean existsByOrganizationEmail(String organizationEmail);

    /** ✅ 기관 + 구독 정보까지 함께 조회 */
    @EntityGraph(attributePaths = {"subscription", "subscription.subscriptionPlan"})
    Optional<Organization> findByOrganizationId(Long organizationId);

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.organizationSubscription")
    List<Organization> findAllWithSubscription();
}
