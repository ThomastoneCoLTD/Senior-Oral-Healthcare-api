package com.kaii.dentix.domain.user.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserLoginIdentifier(String userLoginIdentifier);
    List<User> findByOrganization_OrganizationId(Long organizationId);
    List<User> findByUserPhoneNumberOrUserName(String userPhoneNumber, String userName);
    
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.userToAppServices uta
        LEFT JOIN FETCH uta.appService s
        WHERE u.userId = :userId
    """)
    Optional<User> findByUserIdWithServices(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.organization o
        LEFT JOIN FETCH o.organizationSubscription os
        LEFT JOIN FETCH os.subscriptionPlan sp
        LEFT JOIN FETCH o.subscriptionPlan directSp
        WHERE u.userId = :userId
    """)
    Optional<User> findByIdWithOrganizationAndSubscription(@Param("userId") Long userId);

    // 통계용 쿼리
    long countByOrganization_OrganizationId(Long organizationId);
    long countByOrganization_OrganizationIdAndUserGender(Long organizationId, GenderType userGender);
    long countByOrganization_OrganizationIdAndCreatedAfter(Long organizationId, Date created);
    long countByUserGender(GenderType gender);
    long countByCreatedAfter(Date date);
    long countByOrganization(Organization organization);
    long countByOrganizationAndUserGender(Organization organization, GenderType userGender);
    long countByOrganizationAndCreatedAfter(Organization organization, Date date);

    // 로그인/재발급: refreshToken + lastLoginDate만
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update User u
           set u.userRefreshToken = :refreshToken,
               u.userLastLoginDate = :now
         where u.userId = :userId
    """)
    int updateLoginInfo(@Param("userId") Long userId,
                        @Param("refreshToken") String refreshToken,
                        @Param("now") Date now);

    // 로그아웃: refreshToken null
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update User u
           set u.userRefreshToken = null
         where u.userId = :userId
    """)
    int clearRefreshToken(@Param("userId") Long userId);

    // 회원탈퇴: deleted 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update User u
           set u.deleted = :now
         where u.userId = :userId
    """)
    int revoke(@Param("userId") Long userId, @Param("now") Date now);

    //successCount reset이 자주 충돌하면 이것도 분리 추천
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update User u
           set u.successCount = 0
         where u.userId = :userId
    """)
    int resetSuccessCount(@Param("userId") Long userId);
}
