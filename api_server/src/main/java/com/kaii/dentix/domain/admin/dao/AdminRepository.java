package com.kaii.dentix.domain.admin.dao;

import com.kaii.dentix.domain.admin.domain.Admin;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long>, AdminCustomRepository {

    Optional<Admin> findByAdminLoginIdentifier(String adminIdentifier);
    Optional<Admin> findByAdminPhoneNumber(String adminPhoneNumber);

    /** 기관 + 구독 플랜까지 fetch join으로 함께 조회 */
    @Query("""
        SELECT a FROM Admin a
        LEFT JOIN FETCH a.organization o
        LEFT JOIN FETCH o.organizationSubscription s
        LEFT JOIN FETCH s.subscriptionPlan
        WHERE a.id = :adminId
    """)
    Optional<Admin> findByIdWithOrganizationAndPlan(@Param("adminId") Long adminId);

    @Query("""
    SELECT a FROM Admin a
    LEFT JOIN FETCH a.organization o
    WHERE a.id = :adminId
    """)
    Optional<Admin> findByIdWithOrganization(Long adminId);
}
