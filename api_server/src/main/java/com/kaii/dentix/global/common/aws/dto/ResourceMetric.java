package com.kaii.dentix.global.common.aws.dto;

import java.util.List;

public record ResourceMetric(
        String resourceType,   // EC2 / RDS / S3
        String metricName,     // CPUUtilization, NetworkIn 등
        List<MetricPoint> points
) {}