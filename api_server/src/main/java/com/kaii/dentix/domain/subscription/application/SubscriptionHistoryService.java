package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionHistoryService {

    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    /** ✅ 새 구독 이력 저장 (기관 등록 시, 최초 1회) */
    public void createInitialHistory(Organization organization, SubscriptionPlan plan) {
        SubscriptionHistory history = SubscriptionHistory.builder()
                .organization(organization)
                .subscriptionPlan(plan)
                .startDate(LocalDateTime.now())
                .reason("신규 기관 구독 시작")
                .build();
        subscriptionHistoryRepository.save(history);
    }

    /** ✅ 플랜 변경 시, 기존 이력 종료 후 새 이력 생성 */
    public void recordPlanChange(Organization organization, SubscriptionPlan newPlan, String reason) {
        subscriptionHistoryRepository.findByOrganization_OrganizationIdAndEndDateIsNull(organization.getOrganizationId())
                .ifPresent(h -> h.closeHistory(reason));

        SubscriptionHistory newHistory = SubscriptionHistory.builder()
                .organization(organization)
                .subscriptionPlan(newPlan)
                .startDate(LocalDateTime.now())
                .reason(reason)
                .build();

        subscriptionHistoryRepository.save(newHistory);
    }

    /** ✅ 구독 종료 처리 */
    public void endHistory(Organization organization, String reason) {
        subscriptionHistoryRepository.findByOrganization_OrganizationIdAndEndDateIsNull(organization.getOrganizationId())
                .ifPresent(h -> h.closeHistory(reason));
    }

    /**
     * ✅ 특정 기관의 구독 이력 조회
     */
    public List<SubscriptionHistoryResponse> getSubscriptionHistoryByOrganization(Long organizationId) {
        return subscriptionHistoryRepository
                .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId)
                .stream()
                .map(SubscriptionHistoryResponse::fromEntity)
                .toList();
    }
}