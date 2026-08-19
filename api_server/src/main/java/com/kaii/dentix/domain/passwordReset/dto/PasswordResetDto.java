package com.kaii.dentix.domain.passwordReset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PasswordResetDto {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueResponse {
        private String resetToken;
        private long expiresInSeconds;
        private String loginIdentifier;
    }
}
