package com.kaii.dentix.global.common.aws.controller;


import com.kaii.dentix.global.common.aws.application.CloudWatchService;
import com.kaii.dentix.global.common.aws.dto.AwsMetricsSummaryResponse;
import com.kaii.dentix.global.common.aws.dto.MetricPoint;
import com.kaii.dentix.global.common.aws.dto.ResourceMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.*;
@RestController
@RequestMapping("/api/aws/metrics")
@RequiredArgsConstructor
public class CloudWatchController {

    private final CloudWatchService cloudWatchService;

    @GetMapping("/summary")
    public AwsMetricsSummaryResponse getAwsMetricsSummary() {
        String ec2InstanceId = "i-075a45b255bd21e67";
        String rdsInstanceId = "db-mysql-saas-dev";
        String s3BucketName  = "denti-dev";

        List<ResourceMetric> allMetrics = new ArrayList<>();
        allMetrics.addAll(cloudWatchService.ec2Metrics(ec2InstanceId));
        allMetrics.addAll(cloudWatchService.rdsMetrics(rdsInstanceId));
        allMetrics.addAll(cloudWatchService.s3Metrics(s3BucketName));
        return new AwsMetricsSummaryResponse(allMetrics);
    }
}