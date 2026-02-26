package com.kaii.dentix.domain.agreement.dao;

import com.kaii.dentix.domain.agreement.domain.ServiceAgreementConsent;
import com.kaii.dentix.domain.agreement.dto.ServiceAgreementConsentDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceAgreementConsentRepository extends JpaRepository<ServiceAgreementConsent, Long> {
    ServiceAgreementConsent save(ServiceAgreementConsent serviceAgreementConsent);

    Optional<ServiceAgreementConsent> findByServiceAgreeIdAndUserId(Long serviceAgreeId, Long userId);

    List<ServiceAgreementConsent> findAllByUserId(Long userId);
    @Query("SELECT new com.kaii.dentix.domain.agreement.dto.ServiceAgreementConsentDto$Response(" +
            "s.serviceAgreeId, s.serviceAgreeName, usa.isUserServiceAgree, usa.modified) " +
            "FROM ServiceAgreementConsent usa " +
            "JOIN ServiceAgreement s ON usa.serviceAgreeId = s.serviceAgreeId " +
            "WHERE usa.userId = :userId")
    List<ServiceAgreementConsentDto.Response> findAllByUserIdWithServiceName(@Param("userId") Long userId);


}
