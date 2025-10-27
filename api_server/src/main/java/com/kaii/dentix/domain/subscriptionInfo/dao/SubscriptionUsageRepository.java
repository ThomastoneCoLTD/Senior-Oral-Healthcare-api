package com.kaii.dentix.domain.subscriptionInfo.dao;

import com.kaii.dentix.domain.subscriptionInfo.domain.SubscriptionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionUsage, Long> {
    @Query("""
        SELECT u FROM SubscriptionUsage u
        WHERE u.organization.organizationId = :orgId AND u.active = true
    """)
    Optional<SubscriptionUsage> findActiveByOrganization(Long orgId);

    @Modifying
    @Query("""
        UPDATE SubscriptionUsage u
        SET u.active = false
        WHERE u.organization.organizationId = :orgId AND u.active = true
    """)
    void deactivateActiveUsage(Long orgId);

    // ✅ 기관별 구독정보 조회
    Optional<SubscriptionUsage> findByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
    boolean existsByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
}
