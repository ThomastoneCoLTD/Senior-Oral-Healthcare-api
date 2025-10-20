package com.kaii.dentix.domain.organization.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionHistory;
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

import static com.kaii.dentix.domain.organization.domain.QOrganization.organization;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {
    private final JwtTokenUtil jwtTokenUtil;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final AdminRepository adminRepository;
    //    private final SubscriptionPlanRepository subscriptionPlanRepository;
    //기관등록
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        // ✅ 기관명 중복 체크
        if (organizationRepository.existsByOrganizationName(request.getOrganizationName())) {
            throw new AlreadyDataException("이미 존재하는 기관명입니다.");
        }
        if (organizationRepository.existsByOrganizationPhoneNumber(request.getOrganizationPhoneNumber())) {
            throw new AlreadyDataException("이미 등록된 전화번호입니다.");
        }
        // ✅ 구독 플랜 유효성 체크
        if (request.getSubscriptionPlanId() == null) {
            throw new BadRequestApiException("구독 플랜을 선택해 주세요.");
        }

        // ✅ 구독 플랜 조회
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 플랜입니다."));

        // ✅ usage_reset_date 계산 (플랜 주기별 리셋일)
        LocalDateTime resetDate = null;
        if ("monthly".equalsIgnoreCase(plan.getPlanCycle())) {
            resetDate = LocalDateTime.now().plusMonths(1);
        } else if ("yearly".equalsIgnoreCase(plan.getPlanCycle())) {
            resetDate = LocalDateTime.now().plusYears(1);
        }

        // ✅ 기관 엔티티 생성
        Organization organization = Organization.builder()
                .organizationName(request.getOrganizationName())
                .subscriptionPlan(plan)
                .usageResetDate(resetDate)
                .organizationPhoneNumber(request.getOrganizationPhoneNumber())
                .subscriptionStartDate(LocalDateTime.now()) // ✅ 구독 시작일 추가
                .successCount(0)
                .build();

        // ✅ 저장
        Organization savedOrganization = organizationRepository.save(organization);
        Long adminId = jwtTokenUtil.getCurrentAdminId(); // 현재 로그인한 관리자 ID 가져오기

        // ✅ 6. Admin 테이블 업데이트 (organizationId 세팅)
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        admin.setOrganization(savedOrganization);
        adminRepository.save(admin);
        // ✅ 구독 이력 등록
        SubscriptionHistory history = SubscriptionHistory.builder()
                .organization(savedOrganization)
                .subscriptionPlan(plan)
                .startDate(LocalDateTime.now())
                .endDate(null) // 현재 활성 구독
                .build();

        subscriptionHistoryRepository.save(history);

        // ✅ 응답 DTO 반환
        return OrganizationResponse.builder()
                .organizationId(savedOrganization.getOrganizationId())
                .organizationName(savedOrganization.getOrganizationName())
                .subscriptionPlanId(savedOrganization.getSubscriptionPlan().getId())
                .subscriptionPlanName(savedOrganization.getSubscriptionPlan().getPlanName())
                .organizationPhoneNumber(savedOrganization.getOrganizationPhoneNumber())
                .subscriptionStartDate(savedOrganization.getSubscriptionStartDate())
                .usageResetDate(savedOrganization.getUsageResetDate())
                .successCount(savedOrganization.getSuccessCount())
                .build();
    }
    //기관 상세 조회
    @Transactional
    public OrganizationResponse getOrganizationById(Long id) {
        Organization organization = organizationRepository.findByIdWithPlan(id)
                .orElseThrow(() -> new RuntimeException("기관을 찾을 수 없습니다."));

        // ✅ Lazy 로딩 방지 (사실 필요 없음)
        organization.getSubscriptionPlan().getPlanName();

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

    public OrganizationResponse changeSubscriptionPlan(Long organizationId, Long newPlanId) {
        // ✅ 기관 조회 (fetch join으로 subscriptionPlan까지 미리 로딩)
        Organization organization = organizationRepository.findByIdWithPlan(organizationId)
                .orElseThrow(() -> new RuntimeException("기관을 찾을 수 없습니다."));

        // ✅ 새 구독 플랜 조회
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new RuntimeException("새 구독 플랜을 찾을 수 없습니다."));

        // ✅ 현재 활성 구독 이력 종료
        subscriptionHistoryRepository.findTopByOrganizationOrderByStartDateDesc(organization)
                .ifPresent(history -> {
                    if (history.getEndDate() == null) {
                        history.setEndDate(LocalDateTime.now());
                    }
                });

        // ✅ 새 구독 이력 추가
        SubscriptionHistory newHistory = SubscriptionHistory.builder()
                .organization(organization)
                .subscriptionPlan(newPlan)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .build();
        subscriptionHistoryRepository.save(newHistory);

        // ✅ 기관의 구독 정보 갱신
        organization.updateSubscriptionPlan(newPlan);

        // ✅ 새 플랜 주기에 따라 리셋일 계산
        LocalDateTime resetDate = null;
        if ("monthly".equalsIgnoreCase(newPlan.getPlanCycle())) {
            resetDate = LocalDateTime.now().plusMonths(1);
        } else if ("yearly".equalsIgnoreCase(newPlan.getPlanCycle())) {
            resetDate = LocalDateTime.now().plusYears(1);
        }

        organization.updateUsageResetDate(resetDate);
        organizationRepository.save(organization);
        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .subscriptionPlanId(organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan().getId() : null)
                .subscriptionPlanName(organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan().getPlanName() : null)
                .successCount(organization.getSuccessCount())
                .build();

    }
    public boolean isDuplicate(String organizationName, String organizationPhoneNumber) {
        return organizationRepository.existsByOrganizationNameAndOrganizationPhoneNumber(
                organizationName, organizationPhoneNumber
        );
    }

}