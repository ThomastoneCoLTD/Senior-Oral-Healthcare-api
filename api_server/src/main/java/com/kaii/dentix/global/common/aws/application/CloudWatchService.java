package com.kaii.dentix.global.common.aws;

import com.kaii.dentix.global.common.aws.dto.MetricPoint;
import com.kaii.dentix.global.common.aws.dto.ResourceMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CloudWatchService {

    private final CloudWatchClient cloudWatchClient;

    /**
     * 공통 메서드: CloudWatch 메트릭 데이터 조회
     */
    private List<MetricPoint> getMetricData(String namespace, String metricName, String dimensionName, String dimensionValue) {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace(namespace)
                    .metricName(metricName)
                    .dimensions(Dimension.builder().name(dimensionName).value(dimensionValue).build())
                    .statistics(Statistic.AVERAGE)
                    .period(300) // 5분 단위
                    .startTime(Instant.now().minusSeconds(3600))
                    .endTime(Instant.now())
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            return response.datapoints().stream()
                    .map(dp -> new MetricPoint(dp.timestamp(), dp.average()))
                    .sorted((a, b) -> a.timestamp().compareTo(b.timestamp()))
                    .collect(Collectors.toList());
        } catch (CloudWatchException e) {
            System.err.println("❌ CloudWatch metric fetch error: " + metricName + " -> " + e.awsErrorDetails().errorMessage());
            return List.of();
        }
    }

    /** ✅ EC2 CPU, Memory, Network */
    public List<ResourceMetric> ec2Metrics(String instanceId) {
        return List.of(
                new ResourceMetric("EC2", "CPUUtilization",
                        getMetricData("AWS/EC2", "CPUUtilization", "InstanceId", instanceId)),
                new ResourceMetric("EC2", "MemoryUtilization",
                        getMetricData("CWAgent", "mem_used_percent", "InstanceId", instanceId)), // CloudWatch Agent 필요
                new ResourceMetric("EC2", "NetworkIn",
                        getMetricData("AWS/EC2", "NetworkIn", "InstanceId", instanceId)),
                new ResourceMetric("EC2", "NetworkOut",
                        getMetricData("AWS/EC2", "NetworkOut", "InstanceId", instanceId))
        );
    }

    /** ✅ RDS CPU, Memory, Storage, Connections */
    public List<ResourceMetric> rdsMetrics(String dbInstanceIdentifier) {
        return List.of(
                new ResourceMetric("RDS", "CPUUtilization",
                        getMetricData("AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", dbInstanceIdentifier)),
                new ResourceMetric("RDS", "FreeableMemory",
                        getMetricData("AWS/RDS", "FreeableMemory", "DBInstanceIdentifier", dbInstanceIdentifier)),
                new ResourceMetric("RDS", "FreeStorageSpace",
                        getMetricData("AWS/RDS", "FreeStorageSpace", "DBInstanceIdentifier", dbInstanceIdentifier)),
                new ResourceMetric("RDS", "DatabaseConnections",
                        getMetricData("AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", dbInstanceIdentifier))
        );
    }

    /** ✅ S3 버킷 용량, 객체 수 */
    public List<ResourceMetric> s3Metrics(String bucketName) {
        return List.of(
                new ResourceMetric("S3", "BucketSizeBytes",
                        getMetricData("AWS/S3", "BucketSizeBytes", "BucketName", bucketName)),
                new ResourceMetric("S3", "NumberOfObjects",
                        getMetricData("AWS/S3", "NumberOfObjects", "BucketName", bucketName))
        );
    }

}
