package com.kaii.dentix.domain.agreement.dao;

import com.kaii.dentix.domain.agreement.domain.ServiceAgreement;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceAgreementRepository extends JpaRepository<ServiceAgreement, Long> {

    List<ServiceAgreement> findAll(Sort sort);

    List<ServiceAgreement> findByServiceAgreeSort(Long serviceAgreeSort);
}
