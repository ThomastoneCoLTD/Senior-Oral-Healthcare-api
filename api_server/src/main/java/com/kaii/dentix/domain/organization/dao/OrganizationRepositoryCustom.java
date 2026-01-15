package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.organization.domain.Organization;

import java.util.List;

public interface OrganizationRepositoryCustom {
    List<Organization> findAllOrganizationUsage();
}

