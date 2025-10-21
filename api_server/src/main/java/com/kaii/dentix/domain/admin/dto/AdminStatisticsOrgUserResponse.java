package com.kaii.dentix.domain.admin.dto;

import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsOrgUserResponse {
    private String organizationName;
    private long totalUsers;
    private long maleUsers;
    private long femaleUsers;
    private long newUsers;
    private List<OralCheckResultTypeCount> oralCheckStats;
}
