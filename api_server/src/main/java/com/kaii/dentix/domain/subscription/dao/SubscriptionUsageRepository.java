//package com.kaii.dentix.domain.subscription.dao;
//
//import com.kaii.dentix.domain.subscription.domain.SubscriptionUsage;
//import com.kaii.dentix.domain.subscription.dto.SubscriptionResponseDto;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.Optional;
//
//public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionResponseDto.SubscriptionCycle, Long> {
//    /** 현재 활성 구독 조회 */
//    @Query("""
//        SELECT u FROM SubscriptionUsage u
//        WHERE u.organization.organizationId = :orgId
//        AND u.active = true
//    """)
//    Optional<SubscriptionUsage> findActiveByOrganization(@Param("orgId") Long orgId);
//
//    /** 기존 활성 구독 비활성화 (새 주기 시작 시 호출) */
//    @Modifying(clearAutomatically = true)
//    @Query("""
//        UPDATE SubscriptionUsage u
//        SET u.active = false
//        WHERE u.organization.organizationId = :orgId
//        AND u.active = true
//    """)
//    void deactivateActiveUsage(@Param("orgId") Long orgId);
//
//    //  기관별 구독정보 조회
//    Optional<SubscriptionResponseDto.SubscriptionCycle> findByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
//    boolean existsByOrganization_OrganizationIdAndActiveTrue(Long organizationId);
//}
