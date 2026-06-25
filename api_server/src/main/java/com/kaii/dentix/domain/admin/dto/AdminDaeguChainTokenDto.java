package com.kaii.dentix.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminDaeguChainTokenDto {

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
