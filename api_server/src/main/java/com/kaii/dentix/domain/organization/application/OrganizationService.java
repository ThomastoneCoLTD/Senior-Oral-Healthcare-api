package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationHistoryRepository organizationHistoryRepository;

    @Transactional
    public OrganizationDto.Response createOrganization(OrganizationDto.Request request) {
        if (organizationRepository.existsByOrganizationName(request.getOrganizationName())) {
            throw new AlreadyDataException("이미 존재하는 기관명입니다.");
        }
        if (organizationRepository.existsByOrganizationPhoneNumber(request.getOrganizationPhoneNumber())) {
            throw new AlreadyDataException("이미 등록된 전화번호입니다.");
        }
        if (request.getOrganizationEmail() != null &&
                organizationRepository.existsByOrganizationEmail(request.getOrganizationEmail())) {
            throw new AlreadyDataException("이미 등록된 이메일입니다.");
        }

        Organization organization = Organization.builder()
                .organizationName(request.getOrganizationName())
                .organizationEmail(request.getOrganizationEmail())
                .organizationPhoneNumber(request.getOrganizationPhoneNumber())
                .active(true)
                .build();

        organizationRepository.save(organization);

        Long adminId = jwtTokenUtil.getCurrentAdminId();
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() != null) {
            throw new IllegalStateException("이미 다른 기관에 소속된 관리자입니다.");
        }

        admin.setOrganization(organization);

        LocalDateTime now = LocalDateTime.now();
        return OrganizationDto.Response.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .organizationEmail(organization.getOrganizationEmail())
                .organizationPhoneNumber(organization.getOrganizationPhoneNumber())
                .subscriptionPlanId(null)
                .subscriptionPlanName(null)
                .subscriptionStartDate(now)
                .subscriptionEndDate(null)
                .build();
    }

    @Transactional
    public OrganizationDto.Response getOrganizationById(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
        return OrganizationDto.Response.from(organization);
    }

    @Transactional
    public Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 기관을 찾을 수 없습니다. ID=" + organizationId));
    }

    @Transactional
    public List<OrganizationDto.Response> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAllWithSubscription();

        return organizations.stream()
                .map(org -> OrganizationDto.Response.builder()
                        .organizationId(org.getOrganizationId())
                        .organizationName(org.getOrganizationName())
                        .organizationEmail(org.getOrganizationEmail())
                        .organizationPhoneNumber(org.getOrganizationPhoneNumber())
                        .subscriptionPlanId(org.getOrganizationSubscription() != null ? org.getOrganizationSubscription().getSubscriptionPlan().getId() : null)
                        .subscriptionPlanName(org.getOrganizationSubscription() != null ? org.getOrganizationSubscription().getSubscriptionPlan().getPlanName().name() : null)
                        .subscriptionStartDate(org.getSubscriptionStartDate())
                        .subscriptionEndDate(org.getSubscriptionEndDate())
                        .build())
                .toList();
    }

    @Transactional
    public OrganizationDto.Response findByPhoneNumber(String phoneNumber) {
        Organization organization = organizationRepository.findByPhoneWithPlan(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 전화번호로 등록된 기관이 없습니다."));
        return OrganizationDto.Response.from(organization);
    }

    private void saveHistory(Organization org, String field, String beforeValue, String afterValue, Long adminId) {
        OrganizationHistory history = OrganizationHistory.builder()
                .organization(org)
                .fieldName(field)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .modifiedByAdminId(adminId)
                .modifiedAt(LocalDateTime.now())
                .build();

        organizationHistoryRepository.save(history);
    }

    @Transactional
    public void updateOrganization(Long organizationId, OrganizationDto.UpdateRequest request, Long adminId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));

        if (!organization.getOrganizationName().equals(request.getOrganizationName())) {
            saveHistory(organization,
                    "organizationName",
                    organization.getOrganizationName(),
                    request.getOrganizationName(),
                    adminId);

            organization.setOrganizationName(request.getOrganizationName());
        }

        if (!organization.getOrganizationPhoneNumber().equals(request.getOrganizationPhoneNumber())) {
            saveHistory(organization,
                    "organizationPhoneNumber",
                    organization.getOrganizationPhoneNumber(),
                    request.getOrganizationPhoneNumber(),
                    adminId);

            organization.setOrganizationPhoneNumber(request.getOrganizationPhoneNumber());
        }

        if (!Objects.equals(organization.getOrganizationEmail(), request.getOrganizationEmail())) {
            saveHistory(organization,
                    "organizationEmail",
                    String.valueOf(organization.getOrganizationEmail()),
                    String.valueOf(request.getOrganizationEmail()),
                    adminId);

            organization.setOrganizationEmail(request.getOrganizationEmail());
        }
    }
}
