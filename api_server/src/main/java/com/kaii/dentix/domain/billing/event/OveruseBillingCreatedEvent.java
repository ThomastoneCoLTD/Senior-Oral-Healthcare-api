package com.kaii.dentix.domain.billing.event;

import com.kaii.dentix.domain.billing.domain.Billing;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OveruseBillingCreatedEvent {
    private final Billing billing;
}