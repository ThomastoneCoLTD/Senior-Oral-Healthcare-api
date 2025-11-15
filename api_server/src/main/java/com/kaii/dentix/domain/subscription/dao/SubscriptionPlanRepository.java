package com.kaii.dentix.domain.subscription.dao;

import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    boolean existsByPlanName(String planName);
    /** 활성 상태 + 삭제되지 않은 전체 플랜 정렬 조회 */
    List<SubscriptionPlan> findAllByActiveTrueOrderByPlanSortAsc();
}
