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
import com.kaii.dentix.domain.subscriptionInfo.dao.SubscriptionUsageRepository;
import com.kaii.dentix.domain.subscriptionInfo.domain.SubscriptionUsage;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
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
    private final UserRepository userRepository;
    private final SubscriptionUsageRepository subscriptionUsageRepository;
    //    private final SubscriptionPlanRepository subscriptionPlanRepository;
    //기관등록
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        // ✅ 1. 기관명/전화번호 중복 체크
        if (organizationRepository.existsByOrganizationName(request.getOrganizationName())) {
            throw new AlreadyDataException("이미 존재하는 기관명입니다.");
        }
        if (organizationRepository.existsByOrganizationPhoneNumber(request.getOrganizationPhoneNumber())) {
            throw new AlreadyDataException("이미 등록된 전화번호입니다.");
        }

        // ✅ 2. 구독 플랜 유효성 검증
        if (request.getSubscriptionPlanId() == null) {
            throw new BadRequestApiException("구독 플랜을 선택해 주세요.");
        }

        // ✅ 3. 구독 플랜 조회
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 플랜입니다."));

        // ✅ 4. usage_reset_date 계산 (플랜 주기별)
        LocalDateTime resetDate = switch (plan.getPlanCycle().toLowerCase()) {
            case "monthly" -> LocalDateTime.now().plusMonths(1);
            case "yearly" -> LocalDateTime.now().plusYears(1);
            default -> throw new BadRequestApiException("지원하지 않는 구독 주기입니다.");
        };

        // ✅ 5. 기관 생성
        Organization organization = Organization.builder()
                .organizationName(request.getOrganizationName())
                .subscriptionPlan(plan)
                .organizationPhoneNumber(request.getOrganizationPhoneNumber())
                .subscriptionStartDate(LocalDateTime.now())
                .usageResetDate(resetDate)
                .successCount(0)
                .build();

        Organization savedOrganization = organizationRepository.save(organization);

        // ✅ 6. 현재 로그인 관리자에게 기관 연결
        Long adminId = jwtTokenUtil.getCurrentAdminId();
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));
        admin.setOrganization(savedOrganization);
        adminRepository.save(admin);

        // ✅ 7. 구독 이력 저장
        SubscriptionHistory history = SubscriptionHistory.builder()
                .organization(savedOrganization)
                .subscriptionPlan(plan)
                .startDate(LocalDateTime.now())
                .endDate(null)
                .build();
        subscriptionHistoryRepository.save(history);

        // ✅ 8. 구독 사용 정보 중복 여부 확인
        boolean existsUsage = subscriptionUsageRepository
                .existsByOrganization_OrganizationIdAndActiveTrue(savedOrganization.getOrganizationId());

        if (!existsUsage) {
            // ✅ 9. 사용 정보 생성 (SubscriptionUsage)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodEnd = "monthly".equalsIgnoreCase(plan.getPlanCycle())
                    ? now.plusMonths(1)
                    : now.plusYears(1);

            SubscriptionUsage usage = SubscriptionUsage.builder()
                    .organization(savedOrganization)
                    .subscriptionPlan(plan)
                    .periodStart(now)
                    .periodEnd(periodEnd)
                    .successCount(0)
                    .active(true)
                    .createdAt(now)
                    .build();

            subscriptionUsageRepository.save(usage);
        }

        // ✅ 10. 응답 DTO 반환
        return OrganizationResponse.builder()
                .organizationId(savedOrganization.getOrganizationId())
                .organizationName(savedOrganization.getOrganizationName())
                .organizationPhoneNumber(savedOrganization.getOrganizationPhoneNumber())
                .subscriptionPlanId(plan.getId())
                .subscriptionPlanName(plan.getPlanName())
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

    @Transactional
    public OrganizationResponse changeSubscriptionPlan(Long organizationId, Long newPlanId) {

        // ✅ 기관 조회 (fetch join으로 subscriptionPlan까지 미리 로딩)
        Organization organization = organizationRepository.findByIdWithPlan(organizationId)
                .orElseThrow(() -> new RuntimeException("기관을 찾을 수 없습니다."));

        // ✅ 새 구독 플랜 조회
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new RuntimeException("새 구독 플xxxxxxxxxxxxxxxxxxx랜을 찾을 수 없습니다."));

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

        // ✅ 리셋일, 구독시작일, 사용량 초기화
        organization.updateUsageResetDate(resetDate);
        organization.updateSubscriptionStartDate(LocalDateTime.now());
        organization.updateSuccessCount(0);
        organization.updateUsageRate(0.0);

        // ✅ (여기 추가) 기관 소속 사용자 응답수 초기화
        List<User> users = userRepository.findAllByOrganization(organization);
        for (User user : users) {
            user.setSuccessCount(0);
        }
        userRepository.saveAll(users);

        // ✅ 기관 저장
        organizationRepository.save(organization);

        // ✅ 최신 정보 반환
        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .subscriptionPlanId(
                        organization.getSubscriptionPlan() != null
                                ? organization.getSubscriptionPlan().getId() : null
                )
                .subscriptionPlanName(
                        organization.getSubscriptionPlan() != null
                                ? organization.getSubscriptionPlan().getPlanName() : null
                )
                .successCount(organization.getSuccessCount())
                .subscriptionStartDate(organization.getSubscriptionStartDate())
                .usageResetDate(organization.getUsageResetDate())
                .build();
    }

    public boolean isDuplicate(String organizationName, String organizationPhoneNumber) {
        return organizationRepository.existsByOrganizationNameAndOrganizationPhoneNumber(
                organizationName, organizationPhoneNumber
        );
    }
    public OrganizationResponse getCheckOrganizationById(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("기관 정보가 없습니다"));

        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .subscriptionPlanId(organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan().getId() : null)
                .subscriptionPlanName(organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan().getPlanName() : null)
                .successCount(organization.getSuccessCount())
                .build();
    }
}