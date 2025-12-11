package com.kaii.dentix.domain.billing.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class BillingExcelData {

    private Long organizationId;
    private String organizationName;

    private List<BillingSummaryResponse> summaries;

    // BillingId → BillingOveruseResponse Map
    private Map<Long, BillingOveruseResponse> detailMap;
}