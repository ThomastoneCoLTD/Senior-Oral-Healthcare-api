package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.organization.dao.OrganizationHistoryRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.organization.domain.OrganizationHistory;
import com.kaii.dentix.domain.organization.dto.OrganizationDto;
import com.kaii.dentix.domain.organizationSubscriptionHistory.dao.OrganizationSubscriptionHistoryRepository;
import com.kaii.dentix.domain.organizationSubscriptionHistory.domain.OrganizationSubscriptionHistory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrganizationService {

    private final OrganizationHistoryRepository organizationHistoryRepository;
    private final OrganizationSubscriptionHistoryRepository organizationSubscriptionHistoryRepository;

    /** 일반관리자 - 본인 기관 정보 조회 */
    @Transactional
    public OrganizationDto.Response getMyOrganization(Admin admin) {

        Organization org = admin.getOrganization();
        if (org == null) {
            throw new IllegalArgumentException("해당 관리자는 기관에 소속되어 있지 않습니다.");
        }
        Long organizationId = org.getOrganizationId();

        // 1. 기본 기관 정보 + 현재 구독 상태 매핑
        OrganizationDto.Response response = OrganizationDto.Response.from(org);

        // 2. 전체 구독 이력 조회 (복구됨)
        List<OrganizationSubscriptionHistory> histories =
                organizationSubscriptionHistoryRepository
                        .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId);

        // 3. 이력 엔티티 -> DTO 변환
        List<OrganizationDto.SubscriptionHistoryResponse> historyDtos = histories.stream()
                .map(OrganizationDto.SubscriptionHistoryResponse::fromEntity)
                .toList();

        // 4. 응답 객체에 이력 리스트 주입
        response.setSubscriptionHistories(historyDtos);

        return response;
    }

    /** 일반관리자 - 본인 기관 수정 이력 조회 */
    @Transactional
    public List<OrganizationDto.HistoryResponse> getOrganizationHistory(Long organizationId) {

        List<OrganizationHistory> historyList =
                organizationHistoryRepository
                        .findAllByOrganization_OrganizationIdOrderByModifiedAtDesc(organizationId);

        return historyList.stream()
                .map(h -> OrganizationDto.HistoryResponse.builder()
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