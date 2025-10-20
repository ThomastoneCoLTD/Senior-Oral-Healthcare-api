package com.kaii.dentix.global.common.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class MetricPoint {
    private String time;   // ISO-8601 string
    private Double value;  // metric value
}