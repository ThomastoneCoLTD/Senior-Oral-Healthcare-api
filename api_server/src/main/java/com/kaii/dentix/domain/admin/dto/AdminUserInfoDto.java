package com.kaii.dentix.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminUserInfoDto {

    private Long userId;
    private String userLoginIdentifier;
    private String userName;
    private GenderType userGender;
    private String oralStatus;
    private Date questionnaireDate;
    private OralCheckResultType oralCheckResultTotalType;
    private Date oralCheckDate;
    private YnType isVerify;
    private String oralStatusTitle;
    private List<String> serviceNames; //리스트로 변경

    public AdminUserInfoDto(
            Long userId,
            String userLoginIdentifier,
            String userName,
            GenderType userGender,
            String oralStatusTitle,
            Date questionnaireDate,
            OralCheckResultType oralCheckResultTotalType,
            Date oralCheckDate,
            YnType isVerify,
            String serviceNames //group_concat 문자열
    ) {
        this.userId = userId;
        this.userLoginIdentifier = userLoginIdentifier;
        this.userName = userName;
        this.userGender = userGender;
        this.oralStatusTitle = oralStatusTitle;
        this.questionnaireDate = questionnaireDate;
        this.oralCheckResultTotalType = oralCheckResultTotalType;
        this.oralCheckDate = oralCheckDate;
        this.isVerify = isVerify;

        //문자열을 콤마로 split → List<String> 으로 변환
        if (serviceNames != null && !serviceNames.isEmpty()) {
            this.serviceNames = Arrays.stream(serviceNames.split(","))
                    .map(String::trim)
                    .toList();
        } else {
            this.serviceNames = List.of();
        }
    }

}
