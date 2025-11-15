package com.kaii.dentix.domain.organizationSubscriptionHistory.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dto.OrganizationSubscriptionHistoryResponse;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationSubscriptionHistoryService {
    private final AdminRepository adminRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;

    /**
     * ✅ 기관 관리자 본인 기관의 구독 이력 조회
     */
    @Transactional(readOnly = true)
    public List<OrganizationSubscriptionHistoryResponse> getMySubscriptionHistory(Long adminId) {
        // 1️⃣ 관리자 조회 (기관 함께)
        Admin admin = adminRepository.findByIdWithOrganization(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        Organization organization = admin.getOrganization();
        if (organization == null) {
            throw new IllegalStateException("소속 기관이 존재하지 않습니다.");
        }

        // 2️⃣ 기관의 구독 이력 최신순으로 전체 조회
        List<SubscriptionHistory> histories =
                subscriptionHistoryRepository.findAllByOrganization_OrganizationIdOrderByStartDateDesc(
                        organization.getOrganizationId()
                );

        if (histories.isEmpty()) {
            throw new IllegalStateException("해당 기관의 구독 이력이 존재하지 않습니다.");
        }

        // 3️⃣ DTO 변환
        return histories.stream()
                .map(entity -> OrganizationSubscriptionHistoryResponse.builder()
                        .historyId(entity.getId())
                        .subscriptionPlanName(entity.getSubscriptionPlan().getPlanName().name()) // ✅ Enum → String 변환
                        .planCycle(entity.getSubscriptionPlan().getPlanCycle()) // ✅ Enum → String 변환
                        .price(entity.getSubscriptionPlan().getPrice())
                        .startDate(entity.getStartDate())
                        .endDate(entity.getEndDate())
                        .reason(entity.getReason())
                        .build())
                .toList();
    }
}
