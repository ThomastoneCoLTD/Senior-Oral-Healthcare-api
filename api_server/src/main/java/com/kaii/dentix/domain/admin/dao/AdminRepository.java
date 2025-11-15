package com.kaii.dentix.domain.admin.dao;

import com.kaii.dentix.domain.admin.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long>, AdminCustomRepository {

    Optional<Admin> findByAdminLoginIdentifier(String adminIdentifier);
//    @Query("SELECT a FROM Admin a JOIN FETCH a.organization WHERE a.adminLoginIdentifier = :identifier")
//    Optional<Admin> findByAdminLoginIdentifier(@Param("identifier") String identifier);
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
//    @Query("""
//    SELECT a FROM Admin a
//    JOIN FETCH a.organization o
//    LEFT JOIN FETCH o.subscriptionPlan
//    WHERE a.adminId = :id
//""")
//    Optional<Admin> findByIdWithOrganizationAndPlan(@Param("id") Long id);
//
//    @Query("SELECT a FROM Admin a " +
//            "LEFT JOIN FETCH a.organization o " +
//            "LEFT JOIN FETCH o.subscriptionPlan " +
//            "WHERE a.adminId = :adminId")
//    Optional<Admin> findByIdWithOrganizationAndPlan(@Param("adminId") Long adminId);
}
