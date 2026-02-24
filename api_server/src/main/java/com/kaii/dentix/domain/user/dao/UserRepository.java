package com.kaii.dentix.domain.user.dao;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // 통계용 쿼리
    long countByOrganization_OrganizationId(Long organizationId);
    long countByOrganization_OrganizationIdAndUserGender(Long organizationId, GenderType userGender);
    long countByOrganization_OrganizationIdAndCreatedAfter(Long organizationId, Date created);
    long countByUserGender(GenderType gender);
    long countByCreatedAfter(Date date);
    long countByOrganization(Organization organization);
    long countByOrganizationAndUserGender(Organization organization, GenderType userGender);
    long countByOrganizationAndCreatedAfter(Organization organization, Date date);
}