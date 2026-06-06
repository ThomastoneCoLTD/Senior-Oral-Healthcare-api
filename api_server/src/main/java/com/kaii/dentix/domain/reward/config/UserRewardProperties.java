package com.kaii.dentix.domain.reward.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "user-reward")
public class UserRewardProperties {

    private long oralExerciseCoinAmount = 1L;
    private boolean pointMintEnabled;
    private String pointContractAddress;
    private String pointOwnerAddress;
    private String pointOwnerPrivateKey;
}
