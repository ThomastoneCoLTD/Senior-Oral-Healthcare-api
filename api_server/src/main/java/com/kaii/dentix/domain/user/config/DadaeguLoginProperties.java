package com.kaii.dentix.domain.user.config;

import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dadaegu.login")
public class DadaeguLoginProperties {

    private boolean enabled = false;
    private String siteId;
    private String rsaPrivateKey;
    private String requiredVc = "DaeguMasterVC";

    public boolean isReady() {
        return enabled
                && StringUtils.isNotBlank(siteId)
                && StringUtils.isNotBlank(rsaPrivateKey);
    }
}
