package com.kaii.dentix.domain.billing.dto;

import com.kaii.dentix.domain.billing.domain.BillingStatusHistory;
import com.kaii.dentix.domain.type.BillingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Builder
public class BillingStatusHistoryResponse {

    private Long historyId;
    private Long billingId;
    private BillingStatus oldStatus;
    private BillingStatus newStatus;
    private String changedBy;
    private String memo;
    private Date changedAt;

    public static BillingStatusHistoryResponse from(BillingStatusHistory entity) {
        return BillingStatusHistoryResponse.builder()
                .historyId(entity.getBillingStatusHistoryId())
                .billingId(entity.getBilling().getId())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .changedBy(entity.getChangedBy())
                .memo(entity.getMemo())
                .changedAt(entity.getCreated())
                .build();
    }
}