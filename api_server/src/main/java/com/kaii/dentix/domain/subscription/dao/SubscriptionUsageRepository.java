package com.kaii.dentix.domain.subscription.dao;

import com.kaii.dentix.domain.subscription.dto.SubscriptionResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionResponseDto.SubscriptionCycle, Long> {
    @Query("""
        SELECT u FROM SubscriptionUsageHistory u
        WHERE u.organization.organizationId = :orgId AND u.active = true
    """)
    Optional<SubscriptionResponseDto.SubscriptionCycle> findActiveByOrganization(Long orgId);

    @Modifying
    @Query("""
        UPDATE SubscriptionUsageHistory u
        SET u.active = false
        WHERE u.organization.organizationId = :orgId AND u.active = true
    """)
    void deactivateActiveUsage(Long orgId);

    // ✅ 기관별 구독정보 조회
    Optional<SubscriptionResponseDto.SubscriptionCycle> findByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
    boolean existsByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
}
