package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DaeguDefaultOrganizationService {

    public static final String DAEGU_ORGANIZATION_NAME = "다대구";
    private static final String DAEGU_ORGANIZATION_PHONE = "0530000000";
    private static final String DAEGU_ORGANIZATION_EMAIL = "daegu@soh.local";

    private final OrganizationRepository organizationRepository;

    @Transactional
    public Organization getOrCreate() {
        return organizationRepository.findByOrganizationName(DAEGU_ORGANIZATION_NAME)
                .or(() -> organizationRepository.findByOrganizationPhoneNumber(DAEGU_ORGANIZATION_PHONE))
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .organizationName(DAEGU_ORGANIZATION_NAME)
                        .organizationPhoneNumber(DAEGU_ORGANIZATION_PHONE)
                        .organizationEmail(DAEGU_ORGANIZATION_EMAIL)
                        .organizationAddress("대구광역시")
                        .description("다대구 DID/리워드 연동 기본 기관")
                        .active(true)
                        .build()));
    }
}
