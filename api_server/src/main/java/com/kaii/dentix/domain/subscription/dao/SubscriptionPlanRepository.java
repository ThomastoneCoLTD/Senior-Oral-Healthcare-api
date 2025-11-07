package com.kaii.dentix.domain.subscription.dao;

import com.kaii.dentix.domain.subscription.domain.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    boolean existsByPlanName(String planName);

}
