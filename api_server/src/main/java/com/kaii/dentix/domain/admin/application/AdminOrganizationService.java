package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminOrganizationUsageResponse;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationHistoryResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.subscription.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.UserRole;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Subscription;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AdminOrganizationService {
    private final JwtTokenUtil jwtTokenUtil;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OralCheckRepository oralCheckRepository;
    private final OrganizationHistoryRepository organizationHistoryRepository;

    public Admin getTokenAdmin(HttpServletRequest servletRequest) {
        String token = jwtTokenUtil.getAccessToken(servletRequest);

        UserRole roles = jwtTokenUtil.getRoles(token, TokenType.AccessToken);
        if (!roles.equals(UserRole.ROLE_ADMIN)) throw new UnauthorizedException();

        Long adminId = jwtTokenUtil.getUserId(token, TokenType.AccessToken);
        return adminRepository.findById(adminId).orElseThrow(() -> new NotFoundDataException("존재하지 않는 관리자입니다."));
    }

    /**
     * ✅ 로그인한 어드민의 소속 기관 정보 반환
     */
    public Organization getMyOrganization(Admin admin) {
        Organization organization = admin.getOrganization();
        if (organization == null) {
            throw new IllegalArgumentException("관리자가 소속된 기관이 없습니다.");
        }
        return organization;
    }

    @Transactional
    public List<OrganizationHistoryResponse> getOrganizationHistory(Long organizationId) {

        List<OrganizationHistory> historyList =
                organizationHistoryRepository.findAllByOrganization_OrganizationIdOrderByModifiedAtDesc(organizationId);

        return historyList.stream()
                .map(h -> OrganizationHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .fieldName(h.getFieldName())
                        .beforeValue(h.getBeforeValue())
                        .afterValue(h.getAfterValue())
                        .modifiedByAdminId(h.getModifiedByAdminId())
                        .modifiedAt(h.getModifiedAt().toString())
                        .build()
                )
                .toList();
    }

//    @Transactional
//    public OrganizationUsageResponse getMyOrganizationUsage(Long adminId) {
//
//        Admin admin = adminRepository.findById(adminId)
//                .orElseThrow(() -> new NotFoundDataException("관리자를 찾을 수 없습니다."));
//
//        Organization organization = admin.getOrganization();
//        Subscription subscription = subscriptionService.getCurrentSubscription(organization);
//
//        int successCount = oralCheckRepository.countSuccessByOrganization(organization.getOrganizationId());
//        int max = subscription.getMaxSuccessResponses();
//        int remaining = max - successCount;
//        double usageRate = (double) successCount / max;
//
//        return OrganizationUsageResponse.builder()
//                .subscriptionPlanName(subscription.getPlanName())
//                .maxSuccessResponses(max)
//                .successCount(successCount)
//                .remainingResponses(remaining)
//                .usageRate(usageRate)
//
//                .dailyUsage(oralCheckRepository.countTodayUsage(organization.getOrganizationId()))
//                .weeklyUsage(oralCheckRepository.countThisWeekUsage(organization.getOrganizationId()))
//                .monthlyUsage(oralCheckRepository.countThisMonthUsage(organization.getOrganizationId()))
//
//                .topUsers(oralCheckRepository.findTopUsers(organization.getOrganizationId()))
//                .recentUsages(oralCheckRepository.findRecentUsages(organization.getOrganizationId()))
//                .build();
//    }
//    @Transactional
//    public OrganizationResponse getMyOrganization(HttpServletRequest request) {
//        // ✅ 현재 로그인 관리자 ID 추출
//        Admin admin = this.getTokenAdmin(request);
//
//        // ✅ 관리자 조회
////        Admin admin = adminRepository.findById(adminId)
////                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));
//
//        if (admin.getOrganization() == null) {
//            throw new BadRequestApiException("아직 등록된 기관이 없습니다.");
//        }
//
//        Organization organization = admin.getOrganization();
//
//        // ✅ 응답 DTO 변환
//        return OrganizationResponse.builder()
//                .organizationId(organization.getOrganizationId())
//                .organizationName(organization.getOrganizationName())
//                .organizationPhoneNumber(organization.getOrganizationPhoneNumber())
//                .subscriptionPlanId(organization.getSubscriptionPlan().getId())
//                .subscriptionPlanName(organization.getSubscriptionPlan().getPlanName())
//                .subscriptionStartDate(organization.getSubscriptionStartDate())
//                .usageResetDate(organization.getUsageResetDate())
//                .successCount(organization.getSuccessCount())
//                .build();
//    }
//


//@Transactional
//public Object getMyOrganization(HttpServletRequest request) {
//    // ✅ 현재 로그인 관리자 정보 가져오기
//    Admin admin = this.getTokenAdmin(request);
//
//    // ✅ 슈퍼관리자라면 전체 기관 조회
//    if (admin.getAdminIsSuper() == YnType.Y) {
//        List<OrganizationResponse> allOrganizations = organizationRepository.findAll()
//                .stream()
//                .map(org -> OrganizationResponse.builder()
//                        .organizationId(org.getOrganizationId())
//                        .organizationName(org.getOrganizationName())
//                        .organizationPhoneNumber(org.getOrganizationPhoneNumber())
//                        .subscriptionPlanId(org.getSubscriptionPlan() != null ? org.getSubscriptionPlan().getId() : null)
//                        .subscriptionPlanName(org.getSubscriptionPlan() != null ? org.getSubscriptionPlan().getPlanName().name() : null)
////                        .subscriptionStartDate(org.getSubscriptionStartDate())
////                        .usageResetDate(org.getUsageResetDate())
////                        .successCount(org.getSuccessCount())
//                        .build())
//                .toList();
//
//        return allOrganizations; // ✅ 슈퍼관리자는 전체 목록 반환
//    }
//
//    // ✅ 일반관리자: 자신의 기관만 조회
//    if (admin.getOrganization() == null) {
//        throw new BadRequestApiException("아직 등록된 기관이 없습니다.");
//    }
//
//    Organization organization = admin.getOrganization();
//
//    // ✅ 단일 응답 DTO 변환
//    return OrganizationResponse.builder()
//            .organizationId(organization.getOrganizationId())
//            .organizationName(organization.getOrganizationName())
//            .organizationPhoneNumber(organization.getOrganizationPhoneNumber())
//            .subscriptionPlanId(organization.getSubscriptionPlan().getId())
//            .subscriptionPlanName(organization.getSubscriptionPlan().getPlanName().name())
////            .subscriptionStartDate(organization.getSubscriptionStartDate())
////            .usageResetDate(organization.getUsageResetDate())
////            .successCount(organization.getSuccessCount())
//            .build();
//}
//    public void changeSubscriptionPlan(Long orgId, Long newPlanId) {
//        Organization org = organizationRepository.findById(orgId)
//                .orElseThrow(() -> new IllegalArgumentException("기관이 존재하지 않습니다."));
//
//        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
//                .orElseThrow(() -> new IllegalArgumentException("구독상품이 존재하지 않습니다."));
//
//        // ✅ 1. 플랜 변경
//        org.updateSubscriptionPlan(newPlan);
//
//        // ✅ 2. 구독 시작일 갱신
//        LocalDateTime now = LocalDateTime.now();
//        org.updateSubscriptionStartDate(now);
//
//        // ✅ 3. 해당 날짜 이후 성공 건수 다시 계산
//        long successCount = oralCheckRepository.countSuccessSince(orgId, now);
//        org.increaseUsage();
//
//        organizationRepository.save(org);
//    }
//    @Transactional
//    public List<AdminOrganizationUsageResponse> getAllOrganizationUsage(HttpServletRequest request, Admin admin) {
//        if (admin.getAdminIsSuper() != YnType.Y) {
//            throw new BadRequestApiException("슈퍼관리자만 전체 기관 사용량 조회가 가능합니다.");
//        }
//        return organizationRepository.findAllOrganizationUsage();
//    }

}
