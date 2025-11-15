package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationHistoryRepository extends JpaRepository<OrganizationHistory, Long> {
    List<OrganizationHistory> findAllByOrganization_OrganizationIdOrderByModifiedAtDesc(Long orgId);

}
