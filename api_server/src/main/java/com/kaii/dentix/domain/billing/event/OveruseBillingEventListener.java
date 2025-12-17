package com.kaii.dentix.domain.billing.event;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.global.common.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OveruseBillingEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleOveruseBillingMail(OveruseBillingCreatedEvent event) {

        Billing billing = event.getBilling();
        Organization org = billing.getOrganization();

        if (org.getOrganizationEmail() == null) return;

        emailService.sendBillingNotice(
                org.getOrganizationEmail(),
                org.getOrganizationName(),
                billing.getAmount()
        );
    }
}