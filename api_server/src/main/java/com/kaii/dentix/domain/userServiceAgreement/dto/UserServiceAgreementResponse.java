package com.kaii.dentix.domain.userServiceAgreement.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.type.YnType;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserServiceAgreementResponse {
    private Long serviceAgreeId;
    private YnType isUserServiceAgree;
    private String serviceAgreeName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date date;

    // ✅ JPA DTO 프로젝션용 생성자 (클래스명과 완전히 동일!)
    public UserServiceAgreementResponse(Long serviceAgreeId, String serviceAgreeName, YnType isUserServiceAgree, Date date) {
        this.serviceAgreeId = serviceAgreeId;
        this.isUserServiceAgree = isUserServiceAgree;
        this.serviceAgreeName = serviceAgreeName;
        this.date = date;
    }
}