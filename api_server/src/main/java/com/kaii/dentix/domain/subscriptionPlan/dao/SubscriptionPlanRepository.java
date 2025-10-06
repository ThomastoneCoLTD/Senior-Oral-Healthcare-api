package com.kaii.dentix.domain.subscriptionPlan.dao;

import com.kaii.dentix.domain.subscriptionPlan.domain.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    boolean existsByPlanName(String planName);

}
