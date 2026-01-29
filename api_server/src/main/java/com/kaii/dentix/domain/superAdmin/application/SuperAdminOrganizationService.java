package com.kaii.dentix.domain.superAdmin.application;

import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.billing.dao.BillingRepository;
import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.billing.dto.BillingOveruseResponse;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminDto;
import com.kaii.dentix.domain.superAdmin.dto.SuperAdminStatisticDto;
import com.kaii.dentix.domain.type.BillingType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuperAdminOrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    private final BillingRepository billingRepository;
    private final OralCheckRepository oralCheckRepository;
    private final UserRepository userRepository;
    private final AdminUserCustomRepository adminUserCustomRepository;

    /** 1. 전체 기관 목록 조회 */
    public List<SuperAdminDto.OrganizationListResponse> getAllOrganizations() {
        return organizationRepository.findAll()
                .stream()
                .map(SuperAdminDto.OrganizationListResponse::fromEntity) // 내부 클래스 사용
                .toList();
    }

    /** 2. 기관 상세 정보 조회 */
    public SuperAdminDto.OrganizationDetailResponse getOrganizationDetail(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("해당 기관을 찾을 수 없습니다."));
        return SuperAdminDto.OrganizationDetailResponse.fromEntity(org);
    }

    /** 3. 기관별 구독이력 조회 */
    public List<OrganizationDto.SubscriptionHistoryResponse> getOrganizationSubscriptions(Long organizationId) {

        // 1. DB 조회
        List<OrganizationSubscriptionHistory> histories =
                organizationSubscriptionHistoryRepository
                        .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId);

        // 2. OrganizationDto 내부 클래스를 사용하여 변환
        return histories.stream()
                .map(OrganizationDto.SubscriptionHistoryResponse::fromEntity)
                .toList();
    }

    /**
     * 4. 슈퍼관리자 - 특정 기관의 사용자 사용량 상세 조회
     */
    public OrganizationDto.UsageResponse getOrganizationUsageByOrgId(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관 정보를 찾을 수 없습니다."));

        OrganizationSubscription subscription = organization.getOrganizationSubscription();

        // 구독 정보가 없는 경우에 대한 방어 로직 (Null Safe)
        String planName = (subscription != null && subscription.getSubscriptionPlan() != null)
                ? subscription.getSubscriptionPlan().getPlanName().name()
                : "미구독";

        int max = (subscription != null && subscription.getSubscriptionPlan() != null)
                ? subscription.getSubscriptionPlan().getMaxSuccessResponses()
                : 0;

        // Repository 통계 메서드 호출
        Long successCount = oralCheckRepository.countSuccessByOrganization(organizationId);
        Long remaining = (max > 0) ? (max - successCount) : 0;
        double usageRate = (max > 0) ? ((double) successCount / max) * 100.0 : 0.0;

        //Pageable 자동 처리
        List<OrganizationDto.TopUser> topUsers = oralCheckRepository.findTopUsers(organizationId);
        List<OrganizationDto.RecentUsage> recentUsages = oralCheckRepository.findRecentUsages(organizationId);

        return OrganizationDto.UsageResponse.builder()
                .subscriptionPlanName(planName)
                .maxSuccessResponses(max)
                .successCount(successCount)
                .remainingResponses(remaining)
                .usageRate(usageRate) // 퍼센트 or 소수점 (프론트 협의 필요, 여기선 소수점)
                .dailyUsage(oralCheckRepository.countTodayUsage(organizationId))
                .weeklyUsage(oralCheckRepository.countThisWeekUsage(organizationId))
                .monthlyUsage(oralCheckRepository.countThisMonthUsage(organizationId))
                .topUsers(topUsers)
                .recentUsages(recentUsages)
                .build();
    }

    /**
     * 5. 슈퍼관리자 - 전체 사용자 통계 조회 (전체 + 기관별 상세)
     */
    public SuperAdminStatisticDto.TotalUserStats getSuperAdminTotalUserStatistics(Admin admin) {

        // --- 1. 전체 통계 (Global Stats) ---
        long totalUsers = userRepository.count();
        long maleUsers = userRepository.countByUserGender(GenderType.M);
        long femaleUsers = userRepository.countByUserGender(GenderType.W);

        // Date 변환 (LocalDateTime -> Date)
        LocalDateTime sevenDaysAgoLdt = LocalDateTime.now().minusDays(7);
        Date sevenDaysAgo = Date.from(sevenDaysAgoLdt.atZone(ZoneId.systemDefault()).toInstant());

        long newUsers7Days = userRepository.countByCreatedAfter(sevenDaysAgo);

        // --- 2. 기관별 통계 (Organization Stats) ---
        List<Organization> organizations = organizationRepository.findAll();

        LocalDateTime oneMonthAgoLdt = LocalDateTime.now().minusMonths(1);
        Date oneMonthAgo = Date.from(oneMonthAgoLdt.atZone(ZoneId.systemDefault()).toInstant());

        List<SuperAdminStatisticDto.OrgUserStats> orgStatsList = organizations.stream()
                .map(org -> {
                    // 유저 리포지토리 메서드가 필요함 (기존 코드 유지 가정)
                    long orgTotal = userRepository.countByOrganization(org);
                    long orgMale = userRepository.countByOrganizationAndUserGender(org, GenderType.M);
                    long orgFemale = userRepository.countByOrganizationAndUserGender(org, GenderType.W);
                    long orgNewUsers = userRepository.countByOrganizationAndCreatedAfter(org, oneMonthAgo);

                    return SuperAdminStatisticDto.OrgUserStats.builder()
                            .organizationId(org.getOrganizationId())
                            .organizationName(org.getOrganizationName())
                            .totalUsers(orgTotal)
                            .maleUsers(orgMale)
                            .femaleUsers(orgFemale)
                            .newUsers(orgNewUsers)
                            .build();
                })
                .toList();

        // --- 3. 최종 응답 생성 ---
        return SuperAdminStatisticDto.TotalUserStats.builder()
                .totalUsers(totalUsers)
                .maleUsers(maleUsers)
                .femaleUsers(femaleUsers)
                .newUsers7Days(newUsers7Days)
                .organizationStats(orgStatsList)
                .build();
    }

    /** 6. 기관 현재 구독 정보 (단건) */
    public SuperAdminDto.CurrentSubscriptionResponse getCurrentSubscription(Long orgId) {
        Organization org = organizationRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        if (org.getOrganizationSubscription() == null) {
            throw new NotFoundDataException("현재 구독 상품이 없습니다.");
        }

        return SuperAdminDto.CurrentSubscriptionResponse.builder()
                .organizationId(org.getOrganizationId())
                .organizationName(org.getOrganizationName())
                .subscriptionPlanId(org.getSubscriptionPlan().getId())
                .subscriptionPlanName(org.getSubscriptionPlan().getPlanName().name())
                .startDate(org.getOrganizationSubscription().getSubscriptionStartDate())
                .endDate(org.getOrganizationSubscription().getSubscriptionEndDate())
                .build();
    }

    /** 7. 기관별 결제 내역 조회 (구독+일반) */
    public SuperAdminDto.BillingListResponse getOrganizationBillingForSuperAdmin(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관을 찾을 수 없습니다."));

        List<Billing> billings = billingRepository
                .findByOrganizationAndBillingTypeInOrderByBilledAtDesc(
                        org,
                        List.of(BillingType.SUBSCRIPTION, BillingType.REGULAR)
                );

        // 내부 클래스의 from 메서드 사용
        return SuperAdminDto.BillingListResponse.from(org, billings);
    }

    /** 8. 초과 과금 상세 조회 */
    public BillingOveruseResponse getOveruseDetail(Long billingId) {
        Billing baseBilling = billingRepository.findById(billingId)
                .orElseThrow(() -> new NotFoundDataException("결제 내역을 찾을 수 없습니다."));

        if (!(baseBilling.getBillingType() == BillingType.SUBSCRIPTION ||
                baseBilling.getBillingType() == BillingType.REGULAR ||
                baseBilling.getBillingType() == BillingType.MONTHLY)) {
            throw new IllegalArgumentException("구독 관련 Billing만 조회 가능합니다.");
        }

        // BillingRepository에 추가한 메서드 사용
        List<Billing> overuseList = billingRepository.findByOrganizationAndBillingTypeAndBilledAtBetween(
                baseBilling.getOrganization(),
                BillingType.OVERUSE,
                baseBilling.getPeriodStart(),
                baseBilling.getPeriodEnd()
        );

        long totalAmount = overuseList.stream().mapToLong(Billing::getAmount).sum();

        return BillingOveruseResponse.builder()
                .billingId(baseBilling.getId())
                .planName(baseBilling.getSubscriptionPlan().getPlanName().name())
                .periodStart(baseBilling.getPeriodStart())
                .periodEnd(baseBilling.getPeriodEnd())
                .baseAmount(baseBilling.getAmount())
                .totalOveruseAmount(totalAmount)
                .totalOveruseCount((long) overuseList.size())
                .overuseList(
                        overuseList.stream()
                                .map(BillingOveruseResponse.Item::from)
                                .toList()
                )
                .build();
    }
}