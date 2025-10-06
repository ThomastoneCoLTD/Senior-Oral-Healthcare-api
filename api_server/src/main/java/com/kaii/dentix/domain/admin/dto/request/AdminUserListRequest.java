package com.kaii.dentix.domain.admin.dto.request;

import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter @Setter @SuperBuilder
@AllArgsConstructor
public class AdminUserListRequest extends PageAndSizeRequest {

    private String userIdentifierOrName; // 검색어 (아이디 혹은 이름)

    private OralCheckResultType oralCheckResultTotalType; // 구강 상태

    private String oralStatus; // 문진표 유형

    private GenderType userGender; // 사용자 성별

    private YnType isVerify; // 사용자 인증 여부

    private DatePeriodType allDatePeriod; // 기간 설정 타입 (구강 촬영일 or 문진표 검사일)

    private String startDate; // 기간 설정 시작일

    private String endDate; // 기간 설정 종료일

    private String serviceName;

}
