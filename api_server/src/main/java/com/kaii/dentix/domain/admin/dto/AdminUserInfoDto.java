package com.kaii.dentix.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserInfoDto {

    private Long userId; // 사용자 고유 번호
    private String userLoginIdentifier; // 사용자 아이디
    private String userName; // 사용자 이름
    private GenderType userGender; // 사용자 성별
    private String oralStatus; // 문진표 유형

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date questionnaireDate; // 문진표 검사일

    private OralCheckResultType oralCheckResultTotalType; // 구강 상태 (검진 결과)

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date oralCheckDate; // 구강 촬영일

    private YnType isVerify; // 사용자 인증 여부
    private String patientPhoneNumber; // 환자 연락처

    private String serviceName; // (선택) 사용자 서비스 이름

    // ✅ 현재 QueryDSL select(Projections.constructor(...))와 일치하는 생성자
    public AdminUserInfoDto(
            Long userId,
            String userLoginIdentifier,
            String userName,
            GenderType userGender,
            String oralStatus,
            Date questionnaireDate,
            OralCheckResultType oralCheckResultTotalType,
            Date oralCheckDate,
            YnType isVerify,
            String serviceName
    ) {
        this.userId = userId;
        this.userLoginIdentifier = userLoginIdentifier;
        this.userName = userName;
        this.userGender = userGender;
        this.oralStatus = oralStatus;
        this.questionnaireDate = questionnaireDate;
        this.oralCheckResultTotalType = oralCheckResultTotalType;
        this.oralCheckDate = oralCheckDate;
        this.isVerify = isVerify;
        this.serviceName = serviceName;
    }


}
