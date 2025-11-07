package com.kaii.dentix.domain.organization.application;

import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationUsageService {
    private final OrganizationRepository organizationRepository;

    //성공 응답 기록
    @Transactional
    public int recordSuccessAndGetRemaining(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 기관입니다."));

        SubscriptionPlan plan = organization.getSubscriptionPlan();
        int max = plan.getMaxSuccessResponses();
        int current = organization.getSuccessCount();

        if(current >= max) {
            throw new IllegalStateException(
                    String.format("기관 [%s]은/는 플랜 한도(%d회)를 초과했습니다.",organization.getOrganizationName(), max)
            );
        }

        organization.updateSuccessCount(organization.getSuccessCount() + 1);

        return max - (current + 1);
    }

    //사용량 리셋
    @Transactional
    public void resetUsageIfNeeded(Long organizationId) {
        Organization organization = organizationRepository.findByOrganizationId(organizationId)
                .orElseThrow(()-> new NotFoundDataException("존재하지 않는 기관입니다."));

        if(organization.getUsageResetDate() != null && LocalDateTime.now().isAfter(organization.getUsageResetDate())){
            organization.updateSuccessCount(0);

            //다음 리셋일 계산
            String cycle = organization.getSubscriptionPlan().getPlanCycle();
            if("monthly".equalsIgnoreCase(cycle)){
                organization.updateUsageResetDate(LocalDateTime.now().plusMonths(1));
            } else if("yearly".equalsIgnoreCase(cycle)){
                organization.updateUsageResetDate(LocalDateTime.now().plusYears(1));
            }
        }
    }

    @Transactional
    public int getRemainingResponses(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기관입니다."));
        return org.getSubscriptionPlan().getMaxSuccessResponses() - org.getSuccessCount();
    }

}
