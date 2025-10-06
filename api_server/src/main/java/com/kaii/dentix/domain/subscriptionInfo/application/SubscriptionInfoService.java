package com.kaii.dentix.domain.subscriptionInfo.application;

import com.kaii.dentix.domain.oralCheck.dao.OralCheckRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscriptionInfo.dto.SubscriptionInfoResponse;
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

        // ✅ 기관 전체 성공 응답 수
        long totalSuccessCount = oralCheckRepository.countSuccessByOrganization(organizationId);

        // ✅ 잔여 응답 수 / 사용률
        int max = plan.getMaxSuccessResponses();
        int remaining = Math.max(0, max - (int) totalSuccessCount);
        double usageRate = max == 0 ? 0 : (double) totalSuccessCount / max * 100.0;

        // ✅ 사용자별 성공 응답 수
        List<User> users = userRepository.findByOrganization_OrganizationId(organizationId);
        List<SubscriptionInfoResponse.UserUsage> userUsages = users.stream()
                .map(u -> SubscriptionInfoResponse.UserUsage.builder()
                        .userId(u.getUserId())
                        .userName(u.getUserName())
                        .successCount((int) oralCheckRepository.countByUserIdAndOralCheckAnalysisState(
                                u.getUserId(), OralCheckAnalysisState.SUCCESS))
                        .build())
                .toList();

        return SubscriptionInfoResponse.builder()
                .organizationName(org.getOrganizationName())
                .planName(plan.getPlanName())
                .planCycle(plan.getPlanCycle())
                .price(plan.getPrice())
                .maxSuccessResponses(max)
                .totalSuccessCount((int) totalSuccessCount)
                .remainingCount(remaining)
                .usageRate(Math.round(usageRate * 10) / 10.0)
                .users(userUsages)
                .build();
    }
}