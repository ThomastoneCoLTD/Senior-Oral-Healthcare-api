package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.dao.OrganizationSubscriptionRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.domain.OrganizationSubscription;
import com.kaii.dentix.domain.organization.dto.OrganizationHistoryResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationReResponse;
import com.kaii.dentix.domain.organization.dto.OrganizationResponse;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import com.kaii.dentix.domain.type.SubscriptionStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class AdminOrganizationService {

    private final OrganizationHistoryRepository organizationHistoryRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;

    /** 일반관리자 - 본인 기관 정보 조회 */
    @Transactional
    public OrganizationReResponse getMyOrganization(Admin admin) {

        Organization org = admin.getOrganization();
        if (org == null) {
            throw new IllegalArgumentException("해당 관리자는 기관에 소속되어 있지 않습니다.");
        }

        //현재 구독 (단일 기준)
        OrganizationSubscription subscription =
                organizationSubscriptionRepository
                        .findByOrganization(org)
                        .orElseThrow(() -> new EntityNotFoundException("구독 정보가 없습니다."));

        //과거 구독 이력 (필요하면 함께 내려줌)
        List<OrganizationSubscriptionHistory> histories =
                organizationSubscriptionHistoryRepository
                        .findAllByOrganization_OrganizationIdOrderByStartDateDesc(
                                org.getOrganizationId()
                        );

        return OrganizationReResponse.from(org, subscription, histories);
    }

    /** 일반관리자 - 본인 기관 수정 이력 조회 */
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
}
