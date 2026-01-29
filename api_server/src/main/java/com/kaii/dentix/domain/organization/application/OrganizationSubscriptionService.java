package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationSubscriptionService {

    private final AdminRepository adminRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;

    /** 본인 기관의 구독 정보 조회 */
    @Transactional(readOnly = true)
    public OrganizationDto.SubscriptionResponse getMySubscription(Long adminId) { // 타입 변경

        Admin admin = adminRepository.findByIdWithOrganization(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        Organization organization = admin.getOrganization();
        if (organization == null) {
            throw new IllegalStateException("소속 기관이 없습니다.");
        }

        OrganizationSubscription subscription = organizationSubscriptionRepository
                .findTopByOrganization_OrganizationIdOrderBySubscriptionStartDateDesc(
                        organization.getOrganizationId()
                )
                .orElseThrow(() -> new IllegalArgumentException("현재 활성 구독 정보를 찾을 수 없습니다."));

        SubscriptionPlan plan = subscription.getSubscriptionPlan();

        return OrganizationDto.SubscriptionResponse.fromEntity(
                organization.getOrganizationId(),
                organization.getOrganizationName(),
                organization.getOrganizationPhoneNumber(),
                plan,
                subscription.getSuccessCount(),
                subscription.getSubscriptionStartDate().toLocalDate(),
                subscription.getSubscriptionEndDate().toLocalDate(),
                subscription.getUsageResetDate().toLocalDate()
        );
    }

    @Transactional(readOnly = true)
    public OrganizationSubscription getActiveSubscription(Organization organization) {
        return organizationSubscriptionRepository.findActiveSubscription(organization, LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("현재 활성 구독 상품이 없습니다."));
    }
}