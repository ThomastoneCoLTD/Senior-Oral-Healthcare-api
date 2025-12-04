package com.kaii.dentix.domain.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenType {

    AccessToken(1, "토큰 유형", "Access Token", 2 * 7 * 24 * 60 * 60 * 1000L), // 2시간
    RefreshToken(2, "토큰 유형", "Refresh Token", 2 * 7 * 24 * 60 * 60 * 1000L); // 2주

    private final int id;
    private final String description;
    private final String value;
    private final long validTime;

}
