package com.kaii.dentix.global.common.aws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;


//public class MetricPoint {
//    private String time;   // ISO-8601 string
//    private Double value;  // metric value
//}
public record MetricPoint(Instant timestamp, Double value) {}

