package com.kaii.dentix.domain.organization.application;

import com.amazonaws.services.kms.model.NotFoundException;
import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionChangeRequest;
import com.kaii.dentix.domain.organization.dto.OrganizationSubscriptionResponse;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationSubscriptionService {

    private final AdminOrganizationService adminOrganizationService;
    private final AdminRepository adminRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final AdminService adminService;
    private final BillingRepository billingRepository;
    private final SubscriptionService subscriptionService;
    private final JwtTokenUtil jwtTokenUtil;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    /**
     * ✅ 로그인한 Admin 기준 본인 기관의 구독 정보 조회
     */
    @Transactional(readOnly = true)
    public OrganizationSubscriptionResponse getMySubscription(Long adminId) {
        // ✅ fetch join으로 organization을 세션 내에서 함께 로드
        Admin admin = adminRepository.findByIdWithOrganization(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        Organization organization = admin.getOrganization(); // 이미 초기화된 상태
        if (organization == null) {
            throw new IllegalStateException("소속 기관이 없습니다.");
        }

        OrganizationSubscription subscription = organizationSubscriptionRepository
                .findByOrganization_OrganizationId(organization.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        SubscriptionPlan plan = subscription.getSubscriptionPlan();

        return OrganizationSubscriptionResponse.fromEntity(
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

    public OrganizationSubscription getActiveSubscription(Organization organization) {
        return organizationSubscriptionRepository.findActiveSubscription(organization, LocalDateTime.now())
                .orElseThrow(() -> new IllegalStateException("현재 활성 구독 상품이 없습니다."));
    }

    /** ✅ (1) 기관 관리자용 */
    /**
     * 일반 관리자: 본인 기관의 구독상품 변경
     */
    @Transactional
    public SuccessResponse updateMyOrganizationSubscription(HttpServletRequest request, OrganizationSubscriptionChangeRequest dto) {
        Organization organization = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        // 🔹 JWT에서 현재 사용자 정보 추출 (자신의 기관만 허용)
        Admin adminUser = adminService.getTokenAdmin(request);
        if (!adminUser.getOrganization().getOrganizationId().equals(dto.getOrganizationId())) {
            throw new AccessDeniedException("본인 기관만 수정할 수 있습니다.");
        }

        this.updateSubscriptionCommon(organization, dto.getNewSubscriptionPlanId());
        return new SuccessResponse(200, "기관 구독상품 변경 완료");
    }

    /**
     * 공통 구독 변경 로직 (이력 + 빌링 처리)
     */
    private void updateSubscriptionCommon(Organization organization, Long newPlanId) {

        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new EntityNotFoundException("구독상품을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newEnd = now.plusMonths(1);

        /**
         * 1) 기존 활성 구독 종료
         */
        subscriptionRepository.findActiveSubscription(organization, now)
                .ifPresent(active -> {
                    active.setSubscriptionEndDate(now);
                    active.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(active);
                });

        /**
         * 2) 새 구독(OrganizationSubscription) 생성
         */
        OrganizationSubscription newSubscription = new OrganizationSubscription();
        newSubscription.setOrganization(organization);
        newSubscription.setSubscriptionPlan(newPlan);
        newSubscription.setSubscriptionStartDate(now);
        newSubscription.setSubscriptionEndDate(newEnd);
        newSubscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(newSubscription);

        /**
         * 3) Organization 에는 플랜만 저장 (기간은 Subscription이 관리)
         */
        organization.setSubscriptionPlan(newPlan);
        organizationRepository.save(organization);

        /**
         * 4) Billing 생성 → 반드시 새 Subscription 과 연결
         */
        Billing billing = new Billing();
        billing.setOrganization(organization);
        billing.setSubscription(newSubscription);   // ⭐ 핵심
        billing.setSubscriptionPlan(newPlan);

        billing.setBillingType(BillingType.SUBSCRIPTION);
        billing.setBillingStatus(BillingStatus.PENDING);
        billing.setAmount(newPlan.getPrice());
        billing.setBilledAt(now);
        billing.setPeriodStart(now);
        billing.setPeriodEnd(newEnd);
        billing.setDescription("구독상품 변경 결제");

        billingRepository.save(billing);

        /**
         * 5) SubscriptionHistory도 새로 기록
         */
        SubscriptionHistory history = new SubscriptionHistory();
        history.setOrganization(organization);
        history.setSubscriptionPlan(newPlan);
        history.setStartDate(now);
        history.setEndDate(newEnd);
        history.setReason("구독상품 변경");
        subscriptionHistoryRepository.save(history);
    }

}