package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import org.reactivestreams.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, Long> {

    /** ✅ 기관 ID로 구독 조회 (1:1 관계이므로 단일 결과) */
    Optional<OrganizationSubscription> findByOrganization_OrganizationId(Long organizationId);

    /** ✅ 구독 상태별 조회 (예: ACTIVE, EXPIRED 등) */
    List<OrganizationSubscription> findAllByStatus(SubscriptionStatus status);

    /** ✅ 자동 갱신되는 구독만 조회 (배치/스케줄러용) */
    List<OrganizationSubscription> findAllByAutoRenewTrue();

    /** ✅ 특정 기관의 활성 구독 존재 여부 확인 */
    boolean existsByOrganization_OrganizationIdAndStatus(Long organizationId, SubscriptionStatus status);

    /** ✅ 구독 종료일이 특정 시점 이전이고 autoRenew가 true인 구독 조회 → BillingScheduler에서 사용 */
    List<OrganizationSubscription> findAllBySubscriptionEndDateBeforeAndAutoRenewTrue(LocalDateTime dateTime);

    @Query("""
        select os from OrganizationSubscription os
        join fetch os.organization o
        join fetch os.subscriptionPlan sp
        where o.organizationId = :orgId
          and os.status = com.kaii.dentix.domain.type.SubscriptionStatus.ACTIVE
        order by os.subscriptionStartDate desc
    """)
    List<OrganizationSubscription> findActiveAllByOrgId(@Param("orgId") Long orgId);

    default Optional<OrganizationSubscription> findLatestActiveByOrgId(Long orgId) {
        List<OrganizationSubscription> list = findActiveAllByOrgId(orgId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("""
        SELECT s FROM OrganizationSubscription s
        WHERE s.organization = :organization
        AND :now BETWEEN s.subscriptionStartDate AND s.subscriptionEndDate
        ORDER BY s.subscriptionEndDate DESC
        """)
    Optional<OrganizationSubscription> findActiveSubscription(
            @Param("organization") Organization organization,
            @Param("now") LocalDateTime now
    );
    List<OrganizationSubscription> findAllByOrganizationOrderBySubscriptionStartDateDesc(
            Organization organization
    );
    Optional<OrganizationSubscription> findTopByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
}
