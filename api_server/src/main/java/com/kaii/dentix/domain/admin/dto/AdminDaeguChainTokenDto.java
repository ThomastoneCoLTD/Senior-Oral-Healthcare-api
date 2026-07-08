package com.kaii.dentix.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

public class AdminDaeguChainTokenDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenOption {
        private String tokenName;
        private String contractAddress;
        private String symbol;
        private Long supply;
        private Integer decimals;
        private String owner;
        private String issued;
        private String txHash;
        private String factHash;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "tokenName is required")
        @JsonAlias("tokenName")
        @JsonProperty("token_name")
        private String tokenName;

        @NotNull(message = "supply is required")
        @Positive(message = "supply must be positive")
        private Long supply;
    }
}
