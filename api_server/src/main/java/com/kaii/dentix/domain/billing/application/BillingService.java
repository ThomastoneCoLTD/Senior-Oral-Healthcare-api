package com.kaii.dentix.domain.billing.application;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingHistoryRepository;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.domain.BillingStatusHistory;
import com.kaii.dentix.domain.billing.dto.*;
import com.kaii.dentix.domain.billing.event.OveruseBillingCreatedEvent;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.BillingStatus;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.global.common.dto.PagingDTO;
import com.kaii.dentix.global.common.dto.PagingRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.ZoneId;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final JavaMailSender mailSender;
    private final BillingRepository billingRepository;
    private final OrganizationRepository organizationRepository;
    private final BillingHistoryRepository billingHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;

    /** 일반관리자 - 본인 기관의 미납 청구 목록 조회 */
    @Transactional
    public List<BillingDto.Summary> findAllUnpaidBillings() {
        return billingRepository.findAllByBillingStatus(BillingStatus.PENDING)
                .stream()
                .map(BillingDto.Summary::from)
                .toList();
    }

    /** 일반관리자 - 본인 기관의 빌링 내역 조회 */
    @Transactional
    public BillingDto.ListResponse getBillingsForAdmin(Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) {
            throw new IllegalArgumentException("관리자가 기관에 소속되어 있지 않습니다.");
        }

        List<Billing> billings = billingRepository.findByOrganizationAndBillingTypeIn(
                org,
                List.of(BillingType.MONTHLY, BillingType.SUBSCRIPTION, BillingType.REGULAR)
        );

        return BillingDto.ListResponse.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .billings(billings.stream().map(BillingDto.Summary::from).toList())
                .build();
    }

    /** 일반관리자 - 본인 기관의 빌링 중 초과요금 내역 상세 조회 */
    @Transactional
    public BillingDto.OveruseResponse getOveruseDetails(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found"));

        if (billing.getBillingType() != BillingType.SUBSCRIPTION &&
                billing.getBillingType() != BillingType.REGULAR) {
            throw new IllegalArgumentException("이 Billing은 구독 청구 내역이 아닙니다.");
        }

        List<Billing> overuseList = billingRepository.findOveruseBillingsInPeriod(
                billing.getOrganization().getOrganizationId(),
                BillingType.OVERUSE,
                billing.getPeriodStart(),
                billing.getPeriodEnd()
        );

        long totalOveruseAmount = overuseList.stream()
                .mapToLong(Billing::getAmount)
                .sum();

        return BillingDto.OveruseResponse.builder()
                .billingId(billing.getId())
                .planName(billing.getSubscriptionPlan().getPlanName().name())
                .periodStart(billing.getPeriodStart())
                .periodEnd(billing.getPeriodEnd())
                .baseAmount(billing.getAmount())
                .totalOveruseAmount(totalOveruseAmount)
                .totalOveruseCount(overuseList.size())
                .overuseList(overuseList.stream().map(BillingDto.OveruseResponse.OveruseItem::from).toList())
                .build();
    }

    /** 결제 완료 처리 (markPaid) */
    @Transactional
    public BillingDto.Detail markBillingAsPaid(Long billingId, String paymentRef) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 청구 내역입니다."));

        if (billing.getBillingStatus() == BillingStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 청구 내역입니다.");
        }

        billing.markPaid(paymentRef);
        billingRepository.save(billing);

        return BillingDto.Detail.from(billing);
    }

    /** 초과 사용량 배치 청구 생성 */
    @Transactional
    public Billing createOveruseBatchBilling(Organization organization) {
        if (organization == null) throw new IllegalArgumentException("기관 정보가 없습니다.");

        OrganizationSubscriptionHistory activeHistory = organizationSubscriptionHistoryRepository
                .findByOrganization_OrganizationIdAndEndDateIsNull(organization.getOrganizationId())
                .orElseThrow(() -> new IllegalStateException("활성 구독 이력이 없습니다."));

        SubscriptionPlan plan = activeHistory.getSubscriptionPlan();
        Long unitPrice = plan.getOveruseUnitPrice() != null ? plan.getOveruseUnitPrice().longValue() : 0L;

        boolean exists = billingRepository.existsByOrganizationAndBillingTypeAndBillingStatus(
                organization, BillingType.OVERUSE, BillingStatus.PENDING
        );

        if (exists) return null;

        LocalDateTime now = LocalDateTime.now();
        Billing billing = Billing.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .billingType(BillingType.OVERUSE)
                .billingStatus(BillingStatus.PENDING)
                .amount(unitPrice)
                .billedAt(now)
                .periodStart(now)
                .periodEnd(now)
                .description("AI 구강 분석 초과 1건 요금")
                .build();

        try {
            billingRepository.save(billing);
        } catch (DataIntegrityViolationException e) {
            return null;
        }

        applicationEventPublisher.publishEvent(new OveruseBillingCreatedEvent(billing));
        return billing;
    }

    /** 구독 이력별 초과 요금 조회 */
    @Transactional(readOnly = true)
    public List<BillingDto.SubscriptionOveruse> getOveruseBySubscription(Admin admin) {
        Organization org = admin.getOrganization();
        if (org == null) throw new IllegalArgumentException("소속 기관 없음");

        List<OrganizationSubscriptionHistory> histories = organizationSubscriptionHistoryRepository
                .findAllByOrganization_OrganizationIdOrderByStartDateDesc(org.getOrganizationId());

        List<BillingDto.SubscriptionOveruse> results = new ArrayList<>();

        for (OrganizationSubscriptionHistory h : histories) {
            List<Billing> overuse = billingRepository.findByOrganizationAndBillingTypeIn(
                    org, List.of(BillingType.OVERUSE, BillingType.OVERUSE_BATCH)
            );

            List<Billing> filtered = overuse.stream()
                    .filter(b -> {
                        LocalDateTime date = b.getBilledAt() != null ? b.getBilledAt() :
                                LocalDateTime.ofInstant(b.getCreated().toInstant(), ZoneId.of("Asia/Seoul"));
                        if (date == null) return false;
                        boolean afterStart = date.isEqual(h.getStartDate()) || date.isAfter(h.getStartDate());
                        boolean beforeEnd = (h.getEndDate() == null) || date.isEqual(h.getEndDate()) || date.isBefore(h.getEndDate());
                        return afterStart && beforeEnd;
                    })
                    .toList();

            Long total = filtered.stream().mapToLong(Billing::getAmount).sum();

            results.add(BillingDto.SubscriptionOveruse.builder()
                    .subscriptionId(h.getId())
                    .planName(h.getSubscriptionPlan().getPlanName().name())
                    .periodStart(h.getStartDate())
                    .periodEnd(h.getEndDate())
                    .totalAmount(total)
                    .overuseList(filtered.stream().map(BillingDto.Detail::from).toList())
                    .build());
        }
        return results;
    }

    /** 빌링 상태 변경 + 로그 기록 */
    @Transactional
    public BillingDto.StatusHistoryResponse updateBillingStatus(Long billingId, BillingDto.StatusUpdateRequest request, String changedBy) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new EntityNotFoundException("Billing not found"));

        BillingStatus oldStatus = billing.getBillingStatus();
        BillingStatus newStatus = BillingStatus.valueOf(request.getBillingStatus().toUpperCase());

        if (oldStatus != newStatus) {
            billing.setBillingStatus(newStatus);
            if (newStatus == BillingStatus.PAID && billing.getPaidAt() == null) billing.setPaidAt(LocalDateTime.now());
            if (billing.getBilledAt() == null) billing.setBilledAt(LocalDateTime.now());
        }

        BillingStatusHistory history = billingHistoryRepository.save(
                BillingStatusHistory.of(billing, oldStatus, newStatus, changedBy, request.getMemo())
        );

        return BillingDto.StatusHistoryResponse.from(history);
    }

    /** 기관별 Billing 내역 조회 */
    public List<BillingDto.Detail> getBillingsByOrganization(Long organizationId) {
        return billingRepository.findAllByOrganization_OrganizationIdOrderByBilledAtDesc(organizationId)
                .stream()
                .map(BillingDto.Detail::from)
                .toList();
    }

    /** Billing 단건 조회 */
    @Transactional(readOnly = true)
    public BillingDto.Detail getBillingDetail(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 청구 내역입니다."));
        return BillingDto.Detail.from(billing);
    }

    /** 엑셀 데이터 번들 조회 */
    @Transactional(readOnly = true)
    public BillingDto.ExcelData getBillingExcelBundle(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("기관을 찾을 수 없습니다."));

        List<Billing> billingList = billingRepository.findByOrganizationAndBillingTypeIn(
                org, List.of(BillingType.MONTHLY, BillingType.SUBSCRIPTION, BillingType.REGULAR)
        );

        Map<Long, BillingDto.OveruseResponse> detailMap = new LinkedHashMap<>();
        for (Billing billing : billingList) {
            detailMap.put(billing.getId(), getOveruseDetails(billing.getId()));
        }

        return BillingDto.ExcelData.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .summaries(billingList.stream().map(BillingDto.Summary::from).toList())
                .detailMap(detailMap)
                .build();
    }

    /**
     * 슈퍼관리자 - 특정 기관의 빌링 내역 조회 (ListResponse 반환)
     */
    @Transactional(readOnly = true)
    public BillingDto.ListResponse getBillingListByOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        List<Billing> billings = billingRepository.findByOrganizationAndBillingTypeIn(
                org,
                List.of(BillingType.MONTHLY, BillingType.SUBSCRIPTION, BillingType.REGULAR)
        );

        return BillingDto.ListResponse.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .billings(billings.stream().map(BillingDto.Summary::from).toList())
                .build();
    }

    /**
     * 기관별 빌링 내역 조회 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public BillingDto.PagedResponse getBillingList(Long orgId, String status, String sort, PagingRequest pagingRequest) {
        //정렬 조건 설정 (기간 시작일 기준)
        Sort pageableSort = "ASC".equalsIgnoreCase(sort)
                ? Sort.by("periodStart").ascending()
                : Sort.by("periodStart").descending();

        Pageable pageable = PageRequest.of(pagingRequest.getPage() - 1, pagingRequest.getSize(), pageableSort);

        //검색 조건(Status)에 따라 조회
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("기관을 찾을 수 없습니다."));

        Page<Billing> result;
        if ("ALL".equalsIgnoreCase(status)) {
            // 전체 조회
            result = billingRepository.findByOrganization(org, pageable);
        } else {
            // 상태별 조회
            try {
                BillingStatus statusEnum = BillingStatus.valueOf(status.toUpperCase());
                result = billingRepository.findByOrganizationAndBillingStatus(org, statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 빌링 상태입니다: " + status);
            }
        }

        List<BillingDto.Summary> content = result.getContent().stream()
                .map(BillingDto.Summary::from)
                .toList();

        PagingDTO pagingInfo = PagingDTO.builder()
                .number(result.getNumber())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();

        return BillingDto.PagedResponse.builder()
                .paging(pagingInfo)
                .content(content)
                .build();
    }

    /**
     * 특정 Billing의 상태 변경 이력 목록
     */
    @Transactional(readOnly = true)
    public List<BillingDto.StatusHistoryResponse> getBillingStatusHistories(Long billingId) {
        return billingHistoryRepository
                .findAllByBilling_IdOrderByCreatedDesc(billingId)
                .stream()
                .map(BillingDto.StatusHistoryResponse::from)
                .toList();
    }

}