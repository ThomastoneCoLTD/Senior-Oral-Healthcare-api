package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;

import java.util.List;

public interface OrganizationRepositoryCustom {
    List<AdminOrganizationUsageResponse> findAllOrganizationUsage();

}
