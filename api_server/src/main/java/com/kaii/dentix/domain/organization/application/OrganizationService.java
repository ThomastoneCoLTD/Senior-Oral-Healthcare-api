package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.global.common.error.exception.AlreadyDataException;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
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
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrganizationHistoryRepository organizationHistoryRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;

    /** 일반관리자 - 기관등록 */
    @Transactional
    public OrganizationDto.Response createOrganization(OrganizationDto.Request request) {
        //필수값 체크
        if (request.getSubscriptionPlanId() == null) {
            throw new BadRequestApiException("구독 플랜을 선택해 주세요.");
        }

        //중복 체크
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

        //플랜 조회
        SubscriptionPlan plan = subscriptionPlanRepository.findById(
                request.getSubscriptionPlanId()
        ).orElseThrow(() -> new BadRequestApiException("존재하지 않는 구독 플랜입니다."));

        //기관 생성
        Organization organization = Organization.builder()
                .organizationName(request.getOrganizationName())
                .organizationEmail(request.getOrganizationEmail())
                .organizationPhoneNumber(request.getOrganizationPhoneNumber())
                .active(true)
                .build();

        organizationRepository.save(organization);

        LocalDateTime now = LocalDateTime.now();

        //최초 활성 구독 이력 생성 (endDate = null)
        OrganizationSubscriptionHistory activeHistory =
                OrganizationSubscriptionHistory.builder()
                        .organization(organization)
                        .subscriptionPlan(plan)
                        .startDate(now)
                        .endDate(null)
                        .reason("기관 최초 구독 생성")
                        .successCount(0)
                        .remainingResponses(
                                plan.getMaxSuccessResponses() != null
                                        ? plan.getMaxSuccessResponses()
                                        : 0
                        )
                        .build();

        organizationSubscriptionHistoryRepository.save(activeHistory);

        //Billing 생성 (history 기준)
        Billing billing = Billing.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .billingType(BillingType.REGULAR)
                .billingStatus(BillingStatus.UNPAID)
                .amount(plan.getPrice())
                .billedAt(now)
                .periodStart(now)
                .periodEnd(null)
                .description("기관 등록 시 최초 구독 청구")
                .build();

        billingRepository.save(billing);

        //관리자 연결
        Long adminId = jwtTokenUtil.getCurrentAdminId();
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() != null) {
            throw new IllegalStateException("이미 다른 기관에 소속된 관리자입니다.");
        }

        admin.setOrganization(organization);

        //응답 (history 기준)
        return OrganizationDto.Response.builder()
                .organizationId(organization.getOrganizationId())
                .organizationName(organization.getOrganizationName())
                .organizationEmail(organization.getOrganizationEmail())
                .organizationPhoneNumber(organization.getOrganizationPhoneNumber())
                .subscriptionPlanId(plan.getId())
                .subscriptionPlanName(plan.getPlanName().name())
                .subscriptionStartDate(now)
                .subscriptionEndDate(null)
                .build();
    }

    /** 기관 단건 조회 */
    @Transactional
    public OrganizationDto.Response getOrganizationById(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
        return OrganizationDto.Response.from(organization); // from 메서드 사용
    }

    /** 기관 ID로 기관 단건 조회 */
    @Transactional
    public Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 기관을 찾을 수 없습니다. ID=" + organizationId));
    }

    /** 모든 기관 조회 */
    @Transactional
    public List<OrganizationDto.Response> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAllWithSubscription();

        return organizations.stream()
                .map(org -> OrganizationDto.Response.builder()
                        .organizationId(org.getOrganizationId())
                        .organizationName(org.getOrganizationName())
                        .organizationEmail(org.getOrganizationEmail())
                        .organizationPhoneNumber(org.getOrganizationPhoneNumber())
                        // ... 필요한 필드 매핑 (from 메서드 사용 권장)
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

    /** 일반관리자 - 본인 기관 정보 수정 */
    @Transactional
    public void updateOrganization(Long organizationId, OrganizationDto.UpdateRequest request, Long adminId) { // 타입 변경
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
}