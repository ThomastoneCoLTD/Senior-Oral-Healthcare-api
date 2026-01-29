package com.kaii.dentix.domain.organizationSubscriptionHistory.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationSubscriptionHistoryService {
    private final AdminRepository adminRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    /**
     * 기관 관리자 본인 기관의 구독 이력 조회
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto.SubscriptionHistoryResponse> getMySubscriptionHistory(Long adminId) {

        // 관리자 조회 (기관 함께)
        Admin admin = adminRepository.findByIdWithOrganization(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        Organization organization = admin.getOrganization();
        if (organization == null) {
            throw new IllegalStateException("소속 기관이 존재하지 않습니다.");
        }

        // 기관의 구독 이력 최신순으로 전체 조회
        List<OrganizationSubscriptionHistory> histories =
                organizationSubscriptionHistoryRepository.findAllByOrgIdWithFetch(
                        organization.getOrganizationId()
                );

        // (참고: 빈 리스트일 경우 예외를 던질지, 빈 리스트를 반환할지는 정책에 따라 결정. 여기선 기존 로직 유지)
        if (histories.isEmpty()) {
            throw new IllegalStateException("해당 기관의 구독 이력이 존재하지 않습니다.");
        }

        // [수정] DTO 변환 (fromEntity 사용)
        return histories.stream()
                .map(OrganizationDto.SubscriptionHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto.SubscriptionHistoryResponse> getSubscriptionHistoryByOrganization(Long organizationId) {

        // fetch join으로 subscriptionPlan, organization 모두 가져옴
        List<OrganizationSubscriptionHistory> histories =
                organizationSubscriptionHistoryRepository.findAllByOrgIdWithFetch(organizationId);

        if (histories.isEmpty()) {
            return List.of();
        }

        return histories.stream()
                .map(OrganizationDto.SubscriptionHistoryResponse::fromEntity)
                .toList();
    }

    private final OrganizationSubscriptionHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public OrganizationSubscriptionHistory getActiveHistory(Organization organization) {
        return organizationSubscriptionHistoryRepository
                .findByOrganization_OrganizationIdAndEndDateIsNull(
                        organization.getOrganizationId()
                )
                .orElseThrow(() ->
                        new BadRequestApiException("현재 활성화된 구독 이력이 없습니다.")
                );
    }
}
