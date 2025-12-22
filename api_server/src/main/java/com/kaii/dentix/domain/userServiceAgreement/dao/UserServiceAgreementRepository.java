package com.kaii.dentix.domain.userServiceAgreement.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kaii.dentix.domain.userServiceAgreement.domain.UserServiceAgreement;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreementResponse;

public interface UserServiceAgreementRepository extends JpaRepository<UserServiceAgreement, Long> {
    UserServiceAgreement save(UserServiceAgreement userServiceAgreement);

    Optional<UserServiceAgreement> findByServiceAgreeIdAndUserId(Long serviceAgreeId, Long userId);

    List<UserServiceAgreement> findAllByUserId(Long userId);
    @Query("SELECT new com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreementResponse(" +
            "s.serviceAgreeId, s.serviceAgreeName, usa.isUserServiceAgree, usa.modified) " +
            "FROM UserServiceAgreement usa " +
            "JOIN ServiceAgreement s ON usa.serviceAgreeId = s.serviceAgreeId " +
            "WHERE usa.userId = :userId")
    List<UserServiceAgreementResponse> findAllByUserIdWithServiceName(@Param("userId") Long userId);


}

