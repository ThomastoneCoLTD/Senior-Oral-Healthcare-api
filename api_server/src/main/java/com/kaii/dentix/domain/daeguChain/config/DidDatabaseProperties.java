package com.kaii.dentix.domain.daeguChain.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "did-db")
public class DidDatabaseProperties {

    private String url;
    private String username;
    private String password;
    private String tableName = "DID";
    private String didColumn = "DID";
    private String privateKeyColumn = "private_key";
    private String accountAddressColumn = "account_address";

    public boolean isConfigured() {
        return hasText(url) && hasText(username);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
