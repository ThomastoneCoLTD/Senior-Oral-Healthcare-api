package com.kaii.dentix.domain.organization.dto;

import com.kaii.dentix.domain.type.oral.OralCheckResultType;

import lombok.Getter;


import java.util.Date;

@Getter
public class RecentUsage {

    private final Long oralCheckId;
    private final String userName;
    private final String userLoginIdentifier;
    private final OralCheckResultType resultType;
    private final Date created;

    // 🔥 JPQL new 에서 쓸 생성자 (반드시 public, 파라미터 순서/타입 정확히 일치)
    public RecentUsage(Long oralCheckId,
                       String userName,
                       String userLoginIdentifier,
                       OralCheckResultType resultType,
                       Date created) {
        this.oralCheckId = oralCheckId;
        this.userName = userName;
        this.userLoginIdentifier = userLoginIdentifier;
        this.resultType = resultType;
        this.created = created;
    }
}