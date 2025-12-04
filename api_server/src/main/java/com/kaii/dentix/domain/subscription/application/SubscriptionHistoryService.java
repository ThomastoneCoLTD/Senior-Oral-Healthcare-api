package com.kaii.dentix.domain.subscription.application;

import com.kaii.dentix.domain.organization.dao.OrganizationRepository;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.dao.SubscriptionHistoryRepository;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import com.kaii.dentix.domain.subscription.dto.SubscriptionHistoryResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionHistoryService {

//    private final OrSubscriptionHistoryRepository subscriptionHistoryRepository;

    /**
     * ✅ 특정 기관의 구독 이력 조회
     */
//    public List<SubscriptionHistoryResponse> getSubscriptionHistoryByOrganization(Long organizationId) {
//        return organizationSubscriptionHistoryRepository
//                .findAllByOrganization_OrganizationIdOrderByStartDateDesc(organizationId)
//                .stream()
//                .map(SubscriptionHistoryResponse::fromEntity)
//                .toList();
//    }


}