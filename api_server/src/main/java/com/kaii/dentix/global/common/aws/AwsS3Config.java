package com.kaii.dentix.global.common.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

//@Service
//@RequiredArgsConstructor
@Configuration
public class AwsS3Config {
//    private final S3Client s3;
//
    @Value("${s3.storage.region}")
    private String region;

    @Value("${s3.storage.bucketName}")
    private String bucketName;
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create()) // 환경변수, EC2 role, etc
                .build();
    }
}
