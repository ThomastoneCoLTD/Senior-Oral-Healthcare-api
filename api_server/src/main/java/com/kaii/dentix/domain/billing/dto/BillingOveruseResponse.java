package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.Billing;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingOveruseResponse {

    private Long billingId;
    private String planName;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private long baseAmount;
    private long totalOveruseAmount;
    private long totalOveruseCount;

    private List<Item> overuseList;

    // ⭐⭐⭐ 여기 추가된 부분 ⭐⭐⭐
    public static BillingOveruseResponse of(
            Billing baseBilling,
            List<Billing> overuseList,
            long totalOveruseAmount
    ) {
        return BillingOveruseResponse.builder()
                .billingId(baseBilling.getId())
                .planName(baseBilling.getSubscriptionPlan().getPlanName().name())
                .periodStart(baseBilling.getPeriodStart())
                .periodEnd(baseBilling.getPeriodEnd())
                .baseAmount(baseBilling.getAmount())
                .totalOveruseAmount(totalOveruseAmount)
                .totalOveruseCount((long) overuseList.size())
                .overuseList(
                        overuseList.stream()
                                .map(BillingOveruseResponse.Item::from)
                                .toList()
                )
                .build();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private Long billingId;
        private long amount;
        private LocalDateTime billedAt;
        private String billingStatus;
        private String description;

        public static Item from(Billing billing) {
            return Item.builder()
                    .billingId(billing.getId())
                    .amount(billing.getAmount())
                    .billedAt(billing.getBilledAt())
                    .billingStatus(billing.getBillingStatus().name())
                    .description(billing.getDescription())
                    .build();
        }
    }
}
