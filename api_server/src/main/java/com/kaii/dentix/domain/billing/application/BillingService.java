package com.kaii.dentix.domain.billing.application;

import com.kaii.dentix.domain.admin.application.AdminOrganizationService;
import com.kaii.dentix.domain.admin.application.AdminService;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingHistoryRepository;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.domain.BillingStatusHistory;
import com.kaii.dentix.domain.billing.dto.*;
import com.kaii.dentix.domain.organization.application.OrganizationService;
import com.kaii.dentix.domain.organization.application.OrganizationSubscriptionService;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;

import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.subscription.application.SubscriptionService;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.global.common.EmailService;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.dto.PagingRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {
    private final BillingRepository billingRepository;
    private final AdminOrganizationService adminOrganizationService;
    private final OrganizationRepository organizationRepository;
    private final JavaMailSender mailSender;
    private final EmailService emailService;
    private final AdminService adminService;
    private final BillingHistoryRepository billingHistoryRepository;
    private final OrganizationSubscriptionService organizationSubscriptionService;
    private final OrganizationSubscriptionRepository oraganizationSubscriptionRepository;
    private final BillingRepository billRepository;

    /**
     * ✅ 미결제(PENDING) 상태의 청구 내역 전체 조회
     */
    public List<BillingDto> findAllUnpaidBillings() {
        return billingRepository.findAllByBillingStatus(BillingStatus.PENDING)
                .stream()
                .map(BillingDto::from)
                .toList();
    }

    /**
     * superAdmin : 각 기관별 Billing 내역 조회
     */
    public List<BillingResponse> getBillingsByOrganization(Long organizationId) {
        return billingRepository.findAllByOrganization_OrganizationIdOrderByBilledAtDesc(organizationId)
                .stream()
                .map(BillingResponse::from)
                .toList();
    }

    /**
     * ✅ Billing 단건 조회
     */
    @Transactional(readOnly = true)
    public BillingDetailResponse getBillingDetail(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 청구 내역입니다."));
        return BillingDetailResponse.from(billing);
    }

    /**
     * ✅ 결제 완료 처리 (markPaid)
     */
    public BillingDetailResponse markBillingAsPaid(Long billingId, String paymentRef) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 청구 내역입니다."));

        if (billing.getBillingStatus() == BillingStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 청구 내역입니다.");
        }

        billing.markPaid(paymentRef);
        billingRepository.save(billing);

        return BillingDetailResponse.from(billing);
    }

    @Transactional
    public Billing createOveruseBatchBilling(Organization organization) {
        if (organization == null) {
            throw new IllegalArgumentException("기관 정보가 없습니다.");
        }

        OrganizationSubscription subscription = organization.getOrganizationSubscription();
        if (subscription == null || subscription.getSubscriptionPlan() == null) {
            throw new IllegalArgumentException("기관의 구독 정보가 없습니다.");
        }

        SubscriptionPlan plan = subscription.getSubscriptionPlan();
        int unitPrice = plan.getOveruseUnitPrice();
        // ✅ 요청 시각
        LocalDateTime now = LocalDateTime.now();
        // ✅ 1건 기준으로 과금
        long amount = unitPrice;
        OrganizationSubscription activeSubscription =
                organizationSubscriptionService.getActiveSubscription(organization);

        Billing billing = Billing.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .amount(amount)
                .subscription(activeSubscription)
                .description("AI 구강 분석 초과 1건 요금")
                .billingStatus(BillingStatus.UNPAID)
                .billingType(BillingType.OVERUSE_BATCH)
                .billedAt(now)               // 💡 청구일
                .periodStart(now)            // 💡 이용기간 시작
                .periodEnd(now)              // 💡 이용기간 종료 (1건 단위라 동일)
                .description("AI 구강 분석 초과 1건 요금")
                .build();

        billingRepository.save(billing);

        log.info("기관 [{}] 초과 사용 1건 Billing 생성 완료 (요금: {})", organization.getOrganizationId(), amount);

        // ✅ 이메일 알림 발송 (선택)
        if (organization.getOrganizationEmail() != null) {
            emailService.sendBillingNotice(
                    organization.getOrganizationEmail(),
                    organization.getOrganizationName(),
                    amount
            );
        }

        return billing;
    }

    private void sendBillingEmailNotification(String email, Billing billing) {
        if (email == null || email.isBlank()) {
            log.warn("이메일 주소가 없어 알림 발송 생략");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Dentix] 추가 사용량 청구 안내");
        message.setText(String.format("""
                안녕하세요.

                귀 기관의 구강 분석 서비스 사용량이 초과되어 추가 과금 청구가 발생했습니다.

                ▶ 청구 금액: %d원
                ▶ 청구 유형: %s
                ▶ 생성일: %s

                관리자 페이지에서 결제 내역을 확인해주세요.
                """,
                billing.getAmount(),
                billing.getBillingType(),
                billing.getCreated()
        ));

        mailSender.send(message);
        log.info("이메일 발송 완료 → {}", email);
    }


    /**
     * ✅ 기관 관리자 본인 기관의 모든 빌링 내역 조회
     */
//    @Transactional
//    public BillingListResponse getBillingsForAdmin(Admin admin) {
//
//        Organization organization = adminOrganizationService.getMyOrganization(admin);
//        if (organization == null) {
//            throw new IllegalArgumentException("관리자가 소속된 기관을 찾을 수 없습니다.");
//        }
//
//        // 1️⃣ 기관 Billing 전체 조회
//        List<Billing> billings = billingRepository
//                .findAllByOrganizationOrderByCreatedDesc(organization);
//
//        List<BillingResponse> dtoList = billings.stream()
//                .map(BillingResponse::from)
//                .toList();
//
//        // 2️⃣ 활성 구독상품 조회
//        OrganizationSubscription active =
//                organizationSubscriptionService.getActiveSubscription(organization);
//        if (active == null) {
//            throw new IllegalStateException("현재 활성 구독상품이 없습니다.");
//        }
//
//        LocalDateTime start = active.getSubscriptionStartDate();
//        LocalDateTime end = active.getSubscriptionEndDate();
//
//        // 3️⃣ ⭐ 구독기간 내 생성(created)된 초과요금만 합산
//        Long total = billings.stream()
//                // ⭕ 초과요금만 포함
//                .filter(b -> b.getBillingType() == BillingType.OVERUSE
//                        || b.getBillingType() == BillingType.OVERUSE_BATCH)
//
//                // ⭕ created(Date) 기준으로 기간 필터링
//                .filter(b -> {
//                    if (b.getCreated() == null) return false;
//
//                    // Date → LocalDateTime(KST) 변환
//                    LocalDateTime createdAt = LocalDateTime.ofInstant(
//                            b.getCreated().toInstant(),
//                            ZoneId.of("Asia/Seoul")
//                    );
//
//                    return (createdAt.isEqual(start) || createdAt.isAfter(start)) &&
//                            (createdAt.isEqual(end) || createdAt.isBefore(end));
//                })
//
//                // ⭕ 금액 합산
//                .map(Billing::getAmount)
//                .filter(Objects::nonNull)
//                .mapToLong(Long::longValue)
//                .sum();
//
//        return BillingListResponse.builder()
//                .billings(dtoList)
//                .totalAmount(total)
//                .build();
//    }

    public BillingListResponse getBillingsForAdmin(Admin admin) {

        Organization org = admin.getOrganization();
        if (org == null) {
            throw new IllegalArgumentException("관리자가 기관에 소속되어 있지 않습니다.");
        }

        List<Billing> billings = billingRepository.findByOrganizationAndBillingTypeIn(
                org,
                List.of(BillingType.MONTHLY, BillingType.SUBSCRIPTION, BillingType.REGULAR)
        );

        List<BillingSummaryResponse> summaryList = billings.stream()
                .map(BillingSummaryResponse::from)
                .toList();

        return new BillingListResponse(
                org.getOrganizationId(),
                org.getOrganizationName(),
                summaryList
        );
    }
    @Transactional(readOnly = true)
    public List<SubscriptionOveruseResponse> getOveruseBySubscription(Admin admin) {

        Organization org = adminOrganizationService.getMyOrganization(admin);
        if (org == null) {
            throw new IllegalArgumentException("소속 기관 없음");
        }

        // 1) 이 기관의 모든 구독 정보 불러오기 (최신순)
        List<OrganizationSubscription> subscriptions =
                oraganizationSubscriptionRepository.findAllByOrganizationOrderBySubscriptionStartDateDesc(org);

        List<SubscriptionOveruseResponse> results = new ArrayList<>();

        for (OrganizationSubscription s : subscriptions) {

            LocalDateTime start = s.getSubscriptionStartDate();
            LocalDateTime end = s.getSubscriptionEndDate();

            // 2) 이 구독기간 동안 발생한 "초과요금 Billing"만 조회
            List<Billing> overuse = billingRepository
                    .findBySubscriptionAndBillingTypeIn(
                            s,
                            List.of(BillingType.OVERUSE, BillingType.OVERUSE_BATCH)
                    );

            // 3) created/billedAt 기반 기간 필터링
            List<Billing> filtered = overuse.stream()
                    .filter(b -> {
                        LocalDateTime date = b.getBilledAt();

                        if (date == null && b.getCreated() != null) {
                            date = LocalDateTime.ofInstant(
                                    b.getCreated().toInstant(),
                                    ZoneId.of("Asia/Seoul")
                            );
                        }

                        if (date == null) return false;

                        return (date.isEqual(start) || date.isAfter(start)) &&
                                (date.isEqual(end) || date.isBefore(end));
                    })
                    .toList();

            // 4) 금액 합계
            Long total = filtered.stream()
                    .map(Billing::getAmount)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();

            // 5) Response DTO 생성
            results.add(
                    SubscriptionOveruseResponse.builder()
                            .subscriptionId(s.getId())
                            .planName(s.getSubscriptionPlan().getPlanName().name())
                            .periodStart(start)
                            .periodEnd(end)
                            .totalAmount(total)
                            .overuseList(filtered.stream().map(BillingResponse::from).toList())
                            .build()
            );
        }

        return results;
    }

    /**
     * ✅ 로그인한 관리자 기준으로 본인 기관의 Billing 내역 조회
     */
    public List<BillingResponse> getBillingsForMyOrganization(Admin admin) {

        // 1️⃣ 관리자 소속 기관 조회
        Organization organization = adminOrganizationService.getMyOrganization(admin);
        if (organization == null) {
            throw new IllegalStateException("소속 기관이 존재하지 않습니다.");
        }
    // 2️⃣ 기관의 Billing 내역 최신순 조회
        List<Billing> billings =
                billingRepository.findAllByOrganizationOrderByBilledAtDesc(organization);

        // 3️⃣ Entity → DTO 변환 후 반환
        return billings.stream()
                .map(BillingResponse::from)
                .toList();
    }

    /**
     * 🔹 빌링 상태 변경 + 로그 기록
     */
    @Transactional
    public BillingStatusHistoryResponse updateBillingStatus(Long billingId,
                                                            BillingStatusUpdateRequest request,
                                                            String changedBy) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found: " + billingId));

        BillingStatus oldStatus = billing.getBillingStatus();
        BillingStatus newStatus = BillingStatus.valueOf(request.getBillingStatus().toUpperCase());

        // 같은 상태로 변경 요청이면 그냥 리턴하거나 예외 던질지 선택
        if (oldStatus == newStatus) {
            // 여기서는 그냥 로그만 남기고 그대로 리턴
            BillingStatusHistory history = billingHistoryRepository.save(
                    BillingStatusHistory.of(billing, oldStatus, newStatus, changedBy, request.getMemo())
            );
            return BillingStatusHistoryResponse.from(history);
        }

        // 상태 변경
        billing.setBillingStatus(newStatus);

        // PAID 전환 시 paidAt 자동 세팅
        if (newStatus == BillingStatus.PAID && billing.getPaidAt() == null) {
            billing.setPaidAt(LocalDateTime.now());
        }
        // billedAt 없으면 최초 세팅
        if (billing.getBilledAt() == null) {
            billing.setBilledAt(LocalDateTime.now());
        }

        // 변경 로그 저장
        BillingStatusHistory history = billingHistoryRepository.save(
                BillingStatusHistory.of(billing, oldStatus, newStatus, changedBy, request.getMemo())
        );

        return BillingStatusHistoryResponse.from(history);
    }

    /**
     * 🔹 특정 Billing의 상태 변경 이력 목록
     */
    @Transactional(readOnly = true)
    public List<BillingStatusHistoryResponse> getBillingStatusHistories(Long billingId) {
        return billingHistoryRepository
                .findAllByBilling_IdOrderByCreatedDesc(billingId)
                .stream()
                .map(BillingStatusHistoryResponse::from)
                .toList();
    }

    public Map<String, Object> getBillingList(Long orgId, String status, String sort, PagingRequest pagingRequest) {

        // 1) 상태 필터 변환
        BillingStatus statusEnum = null;
        if (!"ALL".equalsIgnoreCase(status)) {
            statusEnum = BillingStatus.valueOf(status.toUpperCase());
        }

        // 2) 정렬 조건
        Sort pageableSort = sort.equalsIgnoreCase("ASC")
                ? Sort.by("periodStart").ascending()
                : Sort.by("periodStart").descending();

        Pageable pageable = PageRequest.of(pagingRequest.getPage() - 1, pagingRequest.getSize(), pageableSort);

        // 3) DB 조회
        Page<Billing> result = billingRepository.findByOrganizationWithStatusAndPlan(
                orgId,
                status.toUpperCase(),
                statusEnum,
                pageable
        );

        // 4) 응답 DTO 변환
        List<BillingResponse> content = result.getContent()
                .stream()
                .map(BillingResponse::from)
                .toList();

        PagingDTO pagingInfo = PagingDTO.builder()
                .number(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("paging", pagingInfo);

        return response;
    }

    @Transactional
    public BillingOveruseResponse getOveruseDetails(Long billingId) {

        // 1) 구독 Billing 조회
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found"));

        if (billing.getBillingType() != BillingType.SUBSCRIPTION &&
                billing.getBillingType() != BillingType.REGULAR) {
            throw new IllegalArgumentException("이 Billing은 구독 청구 내역이 아닙니다.");
        }

        Long orgId = billing.getOrganization().getOrganizationId();
        LocalDateTime start = billing.getPeriodStart();
        LocalDateTime end = billing.getPeriodEnd();

        // 2) 해당 기간 동안 발생한 초과요금 Billing 조회
        List<Billing> overuseList = billingRepository
                .findByOrganization_OrganizationIdAndBillingTypeAndBilledAtBetween(
                        orgId,
                        BillingType.OVERUSE_BATCH,
                        start,
                        end
                );

        long totalOveruseAmount = overuseList.stream()
                .mapToInt(b -> b.getAmount().intValue())
                .sum();

        return BillingOveruseResponse.of(billing, overuseList, totalOveruseAmount);
    }

    public BillingListResponse getBillingListByOrganization(Long organizationId) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        List<Billing> list = billingRepository.findByOrganizationAndBillingTypeIn(
                org,
                List.of(BillingType.MONTHLY, BillingType.SUBSCRIPTION, BillingType.REGULAR)
        );

        // ⭐ BillingResponse → BillingSummaryResponse 로 변경됨
        List<BillingSummaryResponse> billings = list.stream()
                .map(BillingSummaryResponse::from)
                .toList();

        return new BillingListResponse(
                org.getOrganizationId(),
                org.getOrganizationName(),
                billings
        );
    }

    @Transactional(readOnly = true)
    public BillingExcelData getBillingExcelBundle(Long organizationId) {

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("기관을 찾을 수 없습니다."));

        List<Billing> billingList = billingRepository.findByOrganizationAndBillingTypeIn(
                org,
                List.of(BillingType.MONTHLY, BillingType.SUBSCRIPTION, BillingType.REGULAR)
        );

        List<BillingSummaryResponse> summaries = billingList.stream()
                .map(BillingSummaryResponse::from)
                .toList();

        // Billing ID 별 Detail 시트 전부 생성
        Map<Long, BillingOveruseResponse> detailMap = new LinkedHashMap<>();

        for (Billing b : billingList) {
            BillingOveruseResponse detail = getOveruseDetails(b.getId()); // 기존 함수 재사용
            detailMap.put(b.getId(), detail);
        }

        return BillingExcelData.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .summaries(summaries)
                .detailMap(detailMap)
                .build();
    }

}
