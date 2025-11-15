package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;
import com.kaii.dentix.domain.organization.domain.Organization;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface OrganizationRepositoryCustom {
    List<Organization> findAllOrganizationUsage();
}

