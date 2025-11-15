package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationUpdateRequest;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.dao.SubscriptionUsageRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    private final BillingRepository billingRepository;
    private final OrganizationHistoryRepository organizationHistoryRepository;
//    private final OrganizationRepository organizationRepository;
    //    private final SubscriptionPlanRepository subscriptionPlanRepository;
    /**
     *기관 등록 + 구독 플랜 선택
     */
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        // 1️⃣ 필수 필드 검증
        if (request.getSubscriptionPlanId() == null) {
            throw new BadRequestApiException("구독 플랜을 선택해 주세요.");
        }

        // 2️⃣ 중복 검증
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

        // 3️⃣ 구독 플랜 조회
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new BadRequestApiException("존재하지 않는 구독 플랜입니다."));

        // 4️⃣ 기관 생성
        Organization organization = Organization.builder()
                .organizationName(request.getOrganizationName())
                .organizationEmail(request.getOrganizationEmail())
                .organizationPhoneNumber(request.getOrganizationPhoneNumber())
                .active(true)
                .build();

        organizationRepository.save(organization);

        // 5️⃣ 구독 상태 생성 및 초기화
        OrganizationSubscription subscription = OrganizationSubscription.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .autoRenew(true)
                .build();

        subscription.initializeSubscription();  // ✅ 사용량/리셋일/상태 초기화
        organizationSubscriptionRepository.save(subscription);

        // ✅ 6️⃣ Billing (청구 내역) 생성 — 첫 달 혹은 첫 해 요금
        Billing billing = Billing.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .billingType(BillingType.REGULAR) // 정기 결제 유형
                .billingStatus(BillingStatus.UNPAID) // 결제 대기
                .amount(plan.getPrice())
                .billedAt(LocalDateTime.now())
                .periodStart(subscription.getSubscriptionStartDate())
                .periodEnd(subscription.getSubscriptionEndDate())
                .description("기관 등록 시 자동 청구 생성")
                .build();

        billingRepository.save(billing);

        // ✅ 7️⃣ 구독 이력 생성
        OrganizationSubscriptionHistory history = OrganizationSubscriptionHistory.create(
                organization,
                plan,
                SubscriptionStatus.ACTIVE,
                subscription.getSubscriptionStartDate(),
                subscription.getSubscriptionEndDate(),
                "신규 구독 등록"
        );

        organizationSubscriptionHistoryRepository.save(history);

        // 8️⃣ 관리자 연결
        Long adminId = jwtTokenUtil.getCurrentAdminId();
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() != null) {
            throw new IllegalStateException("이미 다른 기관에 소속된 관리자입니다.");
        }

        admin.setOrganization(organization);

        // ✅ 9️⃣ 응답 DTO
        return OrganizationResponse.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .organizationEmail(organization.getOrganizationEmail())
                .organizationPhoneNumber(organization.getOrganizationPhoneNumber())
                .subscriptionPlanId(plan.getId())
                .subscriptionPlanName(plan.getPlanName().name())
                .subscriptionStartDate(subscription.getSubscriptionStartDate())
                .subscriptionEndDate(subscription.getSubscriptionEndDate())
                .build();
    }


    /**
     *기관 단건 조회 (기관별 상세 정보)_슈퍼관리자
     */
    public OrganizationResponse getOrganizationById(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
        return OrganizationResponse.from(organization);
    }

    /**
     * 기관 ID로 기관 단건 조회
     */
    public Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 기관을 찾을 수 없습니다. ID=" + organizationId));
    }

    /**
     * 모든 기관 조회
     */
    @Transactional
    public List<OrganizationResponse> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAllWithSubscription();

        return organizations.stream()
                .map(org -> OrganizationResponse.builder()
                        .organizationId(org.getOrganizationId())
                        .organizationName(org.getOrganizationName())
                        .organizationEmail(org.getOrganizationEmail())
                        .organizationPhoneNumber(org.getOrganizationPhoneNumber())
                        .subscriptionPlanId(
                                org.getOrganizationSubscription() != null ?
                                        org.getOrganizationSubscription().getSubscriptionPlan().getId() : null)
                        .subscriptionPlanName(
                                org.getOrganizationSubscription() != null ?
                                        org.getOrganizationSubscription().getSubscriptionPlan().getPlanName().name() : null)
                        .subscriptionStartDate(org.getSubscriptionStartDate())
                        .subscriptionEndDate(org.getSubscriptionEndDate())
                        .build())
                .toList();
    }

    @Transactional
    public Organization findByPhoneOrThrow(String phoneNumber) {
        return organizationRepository.findByPhoneWithPlan(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 전화번호로 등록된 기관이 없습니다."));
    }

    @Transactional
    public OrganizationResponse findByPhoneNumber(String phoneNumber) {
        Organization organization = organizationRepository.findByPhoneWithPlan(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 전화번호로 등록된 기관이 없습니다."));

        return OrganizationResponse.from(organization);
    }


//    @Transactional
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

    /**
     * 기관정보 수정
     * @param organizationId
     * @param request
     */
    @Transactional
    public void updateOrganization(Long organizationId, OrganizationUpdateRequest request, Long adminId) {

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));

        // 기관명 변경 로그
        if (!organization.getOrganizationName().equals(request.getOrganizationName())) {
            saveHistory(organization,
                    "organizationName",
                    organization.getOrganizationName(),
                    request.getOrganizationName(),
                    adminId);

            organization.setOrganizationName(request.getOrganizationName());
        }

        // 연락처 변경 로그
        if (!organization.getOrganizationPhoneNumber().equals(request.getOrganizationPhoneNumber())) {
            saveHistory(organization,
                    "organizationPhoneNumber",
                    organization.getOrganizationPhoneNumber(),
                    request.getOrganizationPhoneNumber(),
                    adminId);

            organization.setOrganizationPhoneNumber(request.getOrganizationPhoneNumber());
        }

        // 이메일 변경 로그
        if (!Objects.equals(organization.getOrganizationEmail(), request.getOrganizationEmail())) {
            saveHistory(organization,
                    "organizationEmail",
                    String.valueOf(organization.getOrganizationEmail()),
                    String.valueOf(request.getOrganizationEmail()),
                    adminId);

            organization.setOrganizationEmail(request.getOrganizationEmail());
        }
    }

//    //기관 상세 조회
//    @Transactional
//    public OrganizationResponse getOrganizationById(Long id) {
//        Organization organization = organizationRepository.findByIdWithPlan(id)
//                .orElseThrow(() -> new RuntimeException("기관을 찾을 수 없습니다."));
//
//        // ✅ Lazy 로딩 방지 (사실 필요 없음)
////        organization.getSubscriptionPlan().getPlanName();
//
//        return toResponse(organization);
//    }

//    //기관 조회
//    @Transactional
//    public Page<OrganizationResponse> getAllOrganizations(Pageable pageable) {
//        Page<Organization> page = organizationRepository.findAll(pageable);
//        return page.map(this::toResponse);
//    }

//    /**
//     * 기관 수정
//     */
//    public OrganizationResponse update(Long id, OrganizationUpdateRequest request) {
//        Organization org = organizationRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
//
//        // 기관명 수정 (값이 null 이 아닐 때만)
//        if (request.getOrganizationName() != null) {
//            org.updateOrganizationName(request.getOrganizationName());
//        }
//
//        // 구독 플랜 수정 (값이 null 이 아닐 때만)
//        if (request.getSubscriptionPlanId() != null) {
//            SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
//                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 플랜입니다."));
//            org.updateSubscriptionPlan(plan);
//        }
//
//        // successCount, usageResetDate, deleted 등은 건드리지 않고 유지
//        Organization updated = organizationRepository.save(org);
//
//        return OrganizationResponse.builder()
//                .organizationId(updated.getOrganizationId())
//                .organizationName(updated.getOrganizationName())
//                .subscriptionPlanId(updated.getSubscriptionPlan().getId())
//                .successCount(updated.getSuccessCount())
//                .build();
//
//    }
//
//    //기관 삭제
//    // Soft Delete (플래그만 세움)
//    @Transactional
//    public void softDelete(Long id) {
//        Organization org = organizationRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
//        org.deleteOrganization();
//        organizationRepository.save(org);
//    }
//
//    // Hard Delete (DB에서 실제 삭제)
//    @Transactional
//    public void hardDelete(Long id) {
//        if (!organizationRepository.existsById(id)) {
//            throw new IllegalArgumentException("존재하지 않는 기관입니다.");
//        }
//        organizationRepository.deleteById(id);
//    }
//
//    private OrganizationResponse toResponse(Organization org) {
//        return OrganizationResponse.builder()
//                .organizationId(org.getOrganizationId())
//                .organizationName(org.getOrganizationName())
//                .subscriptionPlanId(org.getSubscriptionPlan() != null ? org.getSubscriptionPlan().getId() : null)
//                .subscriptionPlanName(org.getSubscriptionPlan() != null ? org.getSubscriptionPlan().getPlanName() : null)
//                .successCount(org.getSuccessCount())
//                .build();
//    }
//
//    @Transactional
//    public OrganizationResponse changeSubscriptionPlan(Long organizationId, Long newPlanId) {
//
//        // ✅ 기관 조회 (fetch join으로 subscriptionPlan까지 미리 로딩)
//        Organization organization = organizationRepository.findByIdWithPlan(organizationId)
//                .orElseThrow(() -> new RuntimeException("기관을 찾을 수 없습니다."));
//
//        // ✅ 새 구독 플랜 조회
//        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
//                .orElseThrow(() -> new RuntimeException("새 구독 플xxxxxxxxxxxxxxxxxxx랜을 찾을 수 없습니다."));
//
//        // ✅ 현재 활성 구독 이력 종료
//        subscriptionHistoryRepository.findTopByOrganizationOrderByStartDateDesc(organization)
//                .ifPresent(history -> {
//                    if (history.getEndDate() == null) {
//                        history.setEndDate(LocalDateTime.now());
//                    }
//                });
//
//        // ✅ 새 구독 이력 추가
//        SubscriptionHistory newHistory = SubscriptionHistory.builder()
//                .organization(organization)
//                .subscriptionPlan(newPlan)
//                .startDate(LocalDateTime.now())
//                .endDate(null)
//                .build();
//        subscriptionHistoryRepository.save(newHistory);
//
//        // ✅ 기관의 구독 정보 갱신
//        organization.updateSubscriptionPlan(newPlan);
//
//        // ✅ 새 플랜 주기에 따라 리셋일 계산
//        LocalDateTime resetDate = null;
//        if ("monthly".equalsIgnoreCase(newPlan.getPlanCycle())) {
//            resetDate = LocalDateTime.now().plusMonths(1);
//        } else if ("yearly".equalsIgnoreCase(newPlan.getPlanCycle())) {
//            resetDate = LocalDateTime.now().plusYears(1);
//        }
//
//        // ✅ 리셋일, 구독시작일, 사용량 초기화
////        organization.updateUsageResetDate(resetDate);
////        organization.updateSubscriptionStartDate(LocalDateTime.now());
////        organization.updateSuccessCount(0);
////        organization.updateUsageRate(0.0);
//
//        // ✅ (여기 추가) 기관 소속 사용자 응답수 초기화
//        List<User> users = userRepository.findAllByOrganization(organization);
//        for (User user : users) {
//            user.setSuccessCount(0);
//        }
//        userRepository.saveAll(users);
//
//        // ✅ 기관 저장
//        organizationRepository.save(organization);
//
//        // ✅ 최신 정보 반환
//        return OrganizationResponse.builder()
//                .organizationId(organization.getOrganizationId())
//                .organizationName(organization.getOrganizationName())
//                .subscriptionPlanId(
//                        organization.getSubscriptionPlan() != null
//                                ? organization.getSubscriptionPlan().getId() : null
//                )
//                .subscriptionPlanName(
//                        organization.getSubscriptionPlan() != null
//                                ? organization.getSubscriptionPlan().getPlanName() : null
//                )
//                .successCount(organization.getSuccessCount())
//                .subscriptionStartDate(organization.getSubscriptionStartDate())
//                .usageResetDate(organization.getUsageResetDate())
//                .build();
//    }
//

//    public OrganizationResponse getCheckOrganizationById(Long organizationId) {
//        Organization organization = organizationRepository.findById(organizationId)
//                .orElseThrow(() -> new NotFoundException("기관 정보가 없습니다"));
//
//        return OrganizationResponse.builder()
//                .organizationId(organization.getOrganizationId())
//                .organizationName(organization.getOrganizationName())
//                .subscriptionPlanId(organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan().getId() : null)
//                .subscriptionPlanName(organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan().getPlanName() : null)
//                .successCount(organization.getSuccessCount())
//                .build();
//    }
//
//    /**
//     * ✅ 기관별 구독상품 조회
//     */
//    @Transactional
//    public Organization getOrganizationWithPlan(Long organizationId) {
//        return organizationRepository.findWithSubscriptionPlanById(organizationId)
//                .orElseThrow(() -> new IllegalArgumentException("해당 기관을 찾을 수 없습니다."));
//    }
}