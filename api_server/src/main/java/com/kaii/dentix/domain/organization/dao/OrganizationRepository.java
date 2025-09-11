package com.kaii.dentix.domain.organization.dao;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByOrganizationId(Long organizationId);
    boolean existsByOrganizationName(String name);
    @Modifying
    @Transactional
    @Query("UPDATE Organization o SET o.successCount = o.successCount + 1 WHERE o.organizationId = :orgId")
    void increaseSuccessCount(@Param("orgId") Long orgId);
}
