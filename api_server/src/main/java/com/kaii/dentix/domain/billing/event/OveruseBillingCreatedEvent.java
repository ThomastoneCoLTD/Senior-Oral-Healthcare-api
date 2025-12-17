package com.kaii.dentix.domain.billing.event;

import com.kaii.dentix.domain.billing.domain.Billing;
import com.kaii.dentix.domain.organization.domain.Organization;
import com.kaii.dentix.global.common.EmailService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Getter
@AllArgsConstructor
public class OveruseBillingCreatedEvent {
    private final Billing billing;
}