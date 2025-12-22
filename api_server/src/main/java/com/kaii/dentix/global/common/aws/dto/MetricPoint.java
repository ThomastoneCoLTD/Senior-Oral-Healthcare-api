package com.kaii.dentix.global.common.aws.dto;

import java.time.Instant;

public record MetricPoint(Instant timestamp, Double value) {}

