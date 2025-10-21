package com.kaii.dentix.domain.admin.dto.request;

import com.kaii.dentix.domain.type.DatePeriodType;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AdminStatisticRequest {
    private Long organizationId;
    private DatePeriodType allDatePeriod; // 기간 설정 타입 (구강 촬영일 or 문진표 검사일)

    private String startDate; // 기간 설정 시작일

    private String endDate; // 기간 설정 종료일

}
