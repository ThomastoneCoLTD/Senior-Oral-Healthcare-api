package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>, OrganizationRepositoryCustom {

    // =================================================================
    // 1. 단순 존재 여부 및 기본 조회 (Exists / Find)
    // =================================================================
    boolean existsByOrganizationName(String organizationName);
    boolean existsByOrganizationPhoneNumber(String organizationPhoneNumber);
    boolean existsByOrganizationEmail(String organizationEmail);

    Optional<Organization> findByOrganizationPhoneNumber(String organizationPhoneNumber);

    // =================================================================
    // 2. 연관관계 동시 조회 (Fetch Join / EntityGraph)
    // =================================================================

    /**
     * 기관 ID로 단일 조회 (구독 및 플랜 정보 포함)
     * - EntityGraph를 활용하여 연관된 객체를 한 번의 쿼리로 가져옵니다.
     */
    @EntityGraph(attributePaths = {
            "organizationSubscription",
            "organizationSubscription.subscriptionPlan"
    })
    Optional<Organization> findByOrganizationId(Long organizationId);

    /**
     * 기관 ID로 단일 조회 (JPQL 방식)
     * - EntityGraph 대신 명시적으로 JOIN FETCH를 사용할 때 호출합니다.
     */
    @Query("""
        SELECT o
        FROM Organization o
        LEFT JOIN FETCH o.organizationSubscription os
        LEFT JOIN FETCH os.subscriptionPlan sp
        WHERE o.organizationId = :id
    """)
    Optional<Organization> findWithPlanById(@Param("id") Long id);

    /**
     * 기관 연락처로 단일 조회 (구독 및 플랜 정보 포함)
     */
    @Query("""
        SELECT o
        FROM Organization o
        LEFT JOIN FETCH o.organizationSubscription os
        LEFT JOIN FETCH os.subscriptionPlan sp
        WHERE o.organizationPhoneNumber = :phone
    """)
    Optional<Organization> findByPhoneWithPlan(@Param("phone") String phone);

    /**
     * 전체 기관 목록 조회 (구독 정보 포함)
     */
    @Query("""
        SELECT o 
        FROM Organization o 
        LEFT JOIN FETCH o.organizationSubscription
    """)
    List<Organization> findAllWithSubscription();

}