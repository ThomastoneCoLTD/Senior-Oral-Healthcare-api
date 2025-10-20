package com.kaii.dentix.domain.subscriptionInfo.dao;

import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionInfoRepository extends JpaRepository<SubscriptionPlan, Long> {
    // ✅ 삭제되지 않은 전체 플랜 조회
    List<SubscriptionPlan> findAllByDeletedIsNullOrderByPlanSortAsc();
}
