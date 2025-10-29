package com.kaii.dentix.domain.subscriptionInfo.application;

import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscriptionInfo.dao.SubscriptionInfoRepository;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
import com.kaii.dentix.domain.subscriptionPlan.dao.SubscriptionPlanRepository;
import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class SubscriptionInfoService {

    private final OrganizationRepository organizationRepository;
    private final OralCheckRepository oralCheckRepository;
    private final UserRepository userRepository;
    private final SubscriptionInfoRepository subscriptionInfoRepository;
    /**
     * 기관 구독 정보 + 사용자별 사용량 조회
     */
    /**
     * 기관 구독 정보 + 사용자별 사용량 조회
     */
    @Transactional(readOnly = true)
    public SubscriptionInfoResponse getSubscriptionInfo(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("기관 정보를 찾을 수 없습니다."));

        SubscriptionPlan plan = org.getSubscriptionPlan();
        if (plan == null) {
            throw new NotFoundDataException("기관의 구독 플랜 정보를 찾을 수 없습니다.");
        }

        //기관 전체 사용량 (successCount 컬럼 활용)
        int totalSuccessCount = org.getSuccessCount() != null ? org.getSuccessCount() : 0;

        //제공량 (플랜 제공량)
        int max = plan.getMaxSuccessResponses();

        //잔여량 및 사용률 계산
        int remaining = Math.max(0, max - totalSuccessCount);
        double usageRate = max == 0 ? 0 : (double) totalSuccessCount / max * 100.0;

        //사용자별 성공 응답 수
        List<User> users = userRepository.findByOrganization_OrganizationId(organizationId);
        List<SubscriptionInfoResponse.UserUsage> userUsages = users.stream()
                .map(u -> SubscriptionInfoResponse.UserUsage.builder()
                        .userId(u.getUserId())
                        .userName(u.getUserName())
                        .successCount((int) oralCheckRepository.countByUser_UserIdAndOralCheckAnalysisState(
                                u.getUserId(), OralCheckAnalysisState.SUCCESS))
                        .build())
                .toList();

        //최종 응답
        return SubscriptionInfoResponse.builder()
                .organizationName(org.getOrganizationName())
                .planName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .price(plan.getPrice())

                // 제공량 / 사용량 / 잔여량 / 사용률
                .maxSuccessResponses(max)
                .totalSuccessCount(totalSuccessCount)
                .remainingCount(remaining)
                .usageRate(Math.round(usageRate * 10) / 10.0)

                //구독 시작일 & 갱신(리셋)일
                .subscriptionStartDate(org.getSubscriptionStartDate())
                .usageResetDate(org.getUsageResetDate())

                // 사용자별 이용현황
                .users(userUsages)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionInfoResponse> getAllPlans() {
        return subscriptionInfoRepository.findAllByDeletedIsNullOrderByPlanSortAsc()
                .stream()
                .map(plan -> SubscriptionInfoResponse.builder()
                        .id(plan.getId())
                        .planName(plan.getPlanName())
                        .planCycle(plan.getPlanCycle())
                        .planSort(plan.getPlanSort())
                        .price(plan.getPrice())
                        .maxSuccessResponses(plan.getMaxSuccessResponses())
                        .build())
                .toList();
    }



}
