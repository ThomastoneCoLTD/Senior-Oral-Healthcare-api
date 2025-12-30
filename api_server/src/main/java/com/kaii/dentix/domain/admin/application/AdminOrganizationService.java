package com.kaii.dentix.domain.admin.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.kaii.dentix.domain.admin.domain.Admin;
import jakarta.persistence.EntityNotFoundException;

import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationReResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationHistoryResponse;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;

@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private final OrganizationHistoryRepository organizationHistoryRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;

    /** 일반관리자 - 본인 기관 정보 조회 */
    @Transactional
    public OrganizationReResponse getMyOrganization(Admin admin) {

        Organization org = admin.getOrganization();
        if (org == null) {
            throw new IllegalArgumentException("해당 관리자는 기관에 소속되어 있지 않습니다.");
        }

        Long organizationId = org.getOrganizationId();

        // 현재 구독 = 활성 구독 이력
        OrganizationSubscriptionHistory activeHistory =
                organizationSubscriptionHistoryRepository
                        .findByOrganization_OrganizationIdAndEndDateIsNull(organizationId)
                        .orElseThrow(() -> new EntityNotFoundException("활성 구독 이력이 없습니다."));

        // 전체 구독 이력 (과거 + 현재)
        List<OrganizationSubscriptionHistory> histories =
                organizationSubscriptionHistoryRepository
                        .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId);

        return OrganizationReResponse.from(org, activeHistory, histories);
    }

    /** 일반관리자 - 본인 기관 수정 이력 조회 */
    @Transactional
    public List<OrganizationHistoryResponse> getOrganizationHistory(Long organizationId) {

        List<OrganizationHistory> historyList =
                organizationHistoryRepository
                        .findAllByOrganization_OrganizationIdOrderByModifiedAtDesc(organizationId);

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
}