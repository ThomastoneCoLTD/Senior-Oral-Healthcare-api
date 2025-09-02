package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    //기관등록
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        //기관명 중복체크
        if (organizationRepository.existsByOrganizationName(request.getOrganizationName())) {
            throw new AlreadyDataException("이미 존재하는 기관명입니다.");
        }

        // 구독 플랜 조회
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 플랜입니다."));

        // 구독 플랜 ID 유효성 체크 (필요 시)
        if (request.getSubscriptionPlanId() == null) {
            throw new BadRequestApiException("구독 플랜을 선택해 주세요.");
        }
        // usage_reset_date 계산
        LocalDateTime resetDate = null;
        if ("monthly".equalsIgnoreCase(plan.getPlanCycle())) {
            resetDate = LocalDateTime.now().plusMonths(1);
        } else if ("yearly".equalsIgnoreCase(plan.getPlanCycle())) {
            resetDate = LocalDateTime.now().plusYears(1);
        }

        // 기관 엔티티 생성
        Organization organization = Organization.builder()
                .organizationName(request.getOrganizationName())
                .subscriptionPlan(plan)
                .usageResetDate(resetDate)
                .successCount(0) // 초기값
                .build();

        Organization savedOrganization = organizationRepository.save(organization);
        // 응답 DTO 반환
        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .subscriptionPlanId(savedOrganization.getSubscriptionPlan().getId())
                .subscriptionPlanName(savedOrganization.getSubscriptionPlan().getPlanName())
                .usageResetDate(savedOrganization.getUsageResetDate())
                .successCount(organization.getSuccessCount())
                .build();
    }
    //기관 상세 조회
    @Transactional
    public OrganizationResponse getOrganizationById(Long id) {
        Organization organization = organizationRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("존재하지 않는 기관입니다"));
        return toResponse(organization);
    }

    //기관 조회
    @Transactional
    public Page<OrganizationResponse> getAllOrganizations(Pageable pageable) {
        Page<Organization> page = organizationRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    /**
     * 기관 수정
     */
    public OrganizationResponse update(Long id, OrganizationUpdateRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));

        // 기관명 수정 (값이 null 이 아닐 때만)
        if (request.getOrganizationName() != null) {
            org.updateOrganizationName(request.getOrganizationName());
        }

        // 구독 플랜 수정 (값이 null 이 아닐 때만)
        if (request.getSubscriptionPlanId() != null) {
            SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 플랜입니다."));
            org.updateSubscriptionPlan(plan);
        }

        // successCount, usageResetDate, deleted 등은 건드리지 않고 유지
        Organization updated = organizationRepository.save(org);

        return OrganizationResponse.builder()
                .organizationId(updated.getOrganizationId())
                .organizationName(updated.getOrganizationName())
                .subscriptionPlanId(updated.getSubscriptionPlan().getId())
                .successCount(updated.getSuccessCount())
                .build();

    }

    //기관 삭제
    // Soft Delete (플래그만 세움)
    @Transactional
    public void softDelete(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
        org.deleteOrganization();
        organizationRepository.save(org);
    }

    // Hard Delete (DB에서 실제 삭제)
    @Transactional
    public void hardDelete(Long id) {
        if (!organizationRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 기관입니다.");
        }
        organizationRepository.deleteById(id);
    }

    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .subscriptionPlanId(org.getSubscriptionPlan() != null ? org.getSubscriptionPlan().getId() : null)
                .subscriptionPlanName(org.getSubscriptionPlan() != null ? org.getSubscriptionPlan().getPlanName() : null)
                .successCount(org.getSuccessCount())
                .build();
    }
}