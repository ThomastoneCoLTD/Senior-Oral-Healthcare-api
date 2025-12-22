package com.kaii.dentix.domain.user.dao;

//import com.kaii.dentix.domain.patient.domain.Patient;

import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
//    @Query("SELECT u FROM User u WHERE u.organization.organizationId = :organizationId")
//    List<User> findAllByOrganizationId(@Param("organizationId") Long organizationId);
//    Optional<User> findByPatientId(Long patientId);
    Optional<User> findByUserLoginIdentifier(String userLoginIdentifier);
//    Optional<User> findByUserId(Long userId);

    List<User> findByUserPhoneNumberOrUserName(String userPhoneNumber, String userName);
//    Optional<User> findByUserPhoneNumber(String userPhoneNumber);
//    List<User> findByOrganization_OrganizationId(Long organizationId);
    @Query("""
    SELECT DISTINCT u
    FROM User u
    LEFT JOIN FETCH u.userToAppServices uta
    LEFT JOIN FETCH uta.appService s
    WHERE u.userId = :userId
""")
    Optional<User> findByUserIdWithServices(@Param("userId") Long userId);
    long countByOrganization_OrganizationId(Long organizationId);
    long countByOrganization_OrganizationIdAndUserGender(Long organizationId, GenderType userGender);
    long countByOrganization_OrganizationIdAndCreatedAfter(Long organizationId, Date created);
//    List<User> findAllByOrganization_OrganizationId(Long organizationId);

//    // 기관 ID 기준 사용자 조회 (정확히 Organization과 매핑되는 필드명 확인!)
//    @Query("SELECT u FROM User u WHERE u.organization.organizationId = :organizationId")
//    List<User> findByOrganization_OrganizationId(@Param("organizationId") Long organizationId);
//
//    List<User> findAllByOrganization(Organization organization);
//
//    @Query("SELECT new com.kaii.dentix.domain.admin.dto.AdminUserInfoDto(u.userId, u.userName, o.organizationName, u.userLoginIdentifier, u.userGender, u.created) " +
//            "FROM User u JOIN u.organization o")
//    Page<AdminUserInfoDto> findAllUsers(Pageable pageable);
//
//    @Query("SELECT new com.kaii.dentix.domain.admin.dto.AdminUserInfoDto(u.userId, u.userName, o.organizationName, u.userLoginIdentifier, u.userGender, u.created) " +
//            "FROM User u JOIN u.organization o WHERE o.organizationId = :orgId")
//    Page<AdminUserInfoDto> findAllByOrganizationId(@Param("orgId") Long orgId, Pageable pageable);

    long countByUserGender(GenderType gender);

    long countByCreatedAfter(Date created);

}