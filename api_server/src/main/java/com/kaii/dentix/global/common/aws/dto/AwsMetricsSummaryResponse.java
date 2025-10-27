package com.kaii.dentix.global.common.aws.dto;

import java.util.List;

public record AwsMetricsSummaryResponse(List<ResourceMetric> metrics) {}
