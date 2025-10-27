package com.kaii.dentix.global.common.aws.controller;

import com.kaii.dentix.global.common.aws.CloudWatchService;
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
    public AwsMetricsSummaryResponse getAwsMetricsSummary(
            @RequestParam String ec2InstanceId,
            @RequestParam String rdsInstanceId,
            @RequestParam String s3BucketName
    ) {
        List<ResourceMetric> allMetrics = new ArrayList<>();
        allMetrics.addAll(cloudWatchService.ec2Metrics(ec2InstanceId));
        allMetrics.addAll(cloudWatchService.rdsMetrics(rdsInstanceId));
        allMetrics.addAll(cloudWatchService.s3Metrics(s3BucketName));
        return new AwsMetricsSummaryResponse(allMetrics);
    }
}