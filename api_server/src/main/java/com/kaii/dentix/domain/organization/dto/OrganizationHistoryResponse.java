package com.kaii.dentix.domain.organization.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrganizationHistoryResponse {

    private Long historyId;
    private String fieldName;
    private String beforeValue;
    private String afterValue;
    private Long modifiedByAdminId;
    private String modifiedAt;
}