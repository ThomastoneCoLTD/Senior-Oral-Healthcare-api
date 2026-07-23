package com.kaii.dentix.domain.daeguChain.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

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
    private String didServerBaseUrl = "http://43.201.125.82";
    private String didCreatePath = "/did/create";
    private String tokenServerBaseUrl = "http://43.201.125.82";
    private String tokenCreatePath = "/token/create";
    private String tokenTransferPath = "/token/transfer";
    private String tokenListPath = "/token/token_list";
    private Map<String, String> rewardTokenContracts = new LinkedHashMap<>();

    public String resolveAppKey() {
        return hasText(appKey) ? appKey : token;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
