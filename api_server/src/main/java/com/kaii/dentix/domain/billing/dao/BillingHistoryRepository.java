package com.kaii.dentix.domain.billing.dao;

import com.kaii.dentix.domain.billing.domain.BillingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingHistoryRepository extends JpaRepository<BillingStatusHistory, Long> {
    List<BillingStatusHistory> findAllByBilling_IdOrderByCreatedDesc(Long id);

}
