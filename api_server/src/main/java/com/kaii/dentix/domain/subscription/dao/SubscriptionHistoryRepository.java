package com.kaii.dentix.domain.subscription.dao;


import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.domain.subscription.domain.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {

    // 최신 구독 이력 조회
    Optional<SubscriptionHistory> findTopByOrganizationOrderByStartDateDesc(Organization organization);
    List<SubscriptionHistory> findAllByOrganization_OrganizationIdOrderByStartDateDesc(Long organizationId);

    // 기관별 모든 구독 이력 조회
    List<SubscriptionHistory> findAllByOrganizationOrderByStartDateDesc(Organization organization);
}
