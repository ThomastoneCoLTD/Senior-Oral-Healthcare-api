package com.kaii.dentix.domain.daeguChain.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "daegu-chain")
public class DaeguChainProperties {

    private String baseUrl = "https://www.daegu.go.kr/daeguchain";
    private String apiBaseUrl;
    private String apiVersion = "v2";
    private String chain = "dchain";
    private String appKey;
    private String token;
    private String tokenOwnerAddress;
    private String tokenOwnerPrivateKey;
    private String tokenSymbol = "MYT";
    private Integer tokenDecimals = 18;
    private Boolean tokenMintable = true;
    private Boolean tokenLockable = true;
    private String didServerBaseUrl = "http://localhost:5000";
    private String didCreatePath = "/did/create";
    private String tokenServerBaseUrl = "http://localhost:5000";
    private String tokenCreatePath = "/token/create";
    private String tokenTransferPath = "/token/transfer";
    private String tokenListPath = "/token/token_list";

    public String resolveAppKey() {
        return hasText(appKey) ? appKey : token;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
