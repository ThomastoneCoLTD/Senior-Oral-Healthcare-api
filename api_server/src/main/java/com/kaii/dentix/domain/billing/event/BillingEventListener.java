//package com.kaii.dentix.domain.billing.event;
//
//import com.kaii.dentix.domain.billing.application.BillingService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//
//@Component
//@RequiredArgsConstructor
//public class BillingEventListener {
//
//    private final BillingService billingService;
//
//    @Async
//    @TransactionalEventListener(
//            phase = TransactionPhase.AFTER_COMMIT
//    )
//    public void handleOrganizationCreated(OrganizationCreatedEvent event) {
//        billingService.createInitialBilling(event.getOrganizationId());
//    }
//}