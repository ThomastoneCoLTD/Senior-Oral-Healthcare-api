package com.kaii.dentix.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

@Configuration
public class CloudWatchConfig {

    @Value("${aws.region:ap-southeast-1}")
    private String region;

    @Bean
    public CloudWatchClient cloudWatchClient() {
        System.out.println("[CloudWatchConfig] Using IAM Role for CloudWatch. region=" + region);

        return CloudWatchClient.builder()
                .region(Region.of(region))
                .build();  // IAM Role 자동 사용
    }
}
