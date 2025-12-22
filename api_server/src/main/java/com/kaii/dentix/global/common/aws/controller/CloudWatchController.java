package com.kaii.dentix.global.common.aws.controller;

import java.util.List;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaii.dentix.global.common.aws.dto.ResourceMetric;
import com.kaii.dentix.global.common.aws.application.CloudWatchService;
import com.kaii.dentix.global.common.aws.dto.AwsMetricsSummaryResponse;

@RestController
@RequestMapping("/api/aws/metrics")
@RequiredArgsConstructor
public class CloudWatchController {

    private final CloudWatchService cloudWatchService;

    @GetMapping("/summary")
    public AwsMetricsSummaryResponse getAwsMetricsSummary() {
        // ASG 이름으로 조회
        String asgName = "denti-global-backend-asg";
        String rdsInstanceId = "aurora-denti-global-dev-instance-1";
        String s3BucketName  = "denti-global-singapore";

        List<ResourceMetric> allMetrics = new ArrayList<>();

        // ec2Metrics(instanceId) 대신 asgMetrics(asgName) 호출!
        allMetrics.addAll(cloudWatchService.asgMetrics(asgName));
        allMetrics.addAll(cloudWatchService.rdsMetrics(rdsInstanceId));
        allMetrics.addAll(cloudWatchService.s3Metrics(s3BucketName));

        return new AwsMetricsSummaryResponse(allMetrics);
    }
}