//package com.kaii.dentix.global.common.aws;
//
//import com.kaii.dentix.global.common.aws.dto.MetricPoint;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
//import software.amazon.awssdk.services.cloudwatch.model.*;
//
//import java.time.Instant;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class CloudWatchService {
//
//    private final CloudWatchClient cloudWatchClient;
//    private final CloudWatchClient cw;
//    /**
//     * EC2 CPU 사용률 (getMetricStatistics 버전)
//     */
//    public List<Map<String, Object>> getEc2CpuMetricStatistics(String instanceId) {
//        Instant end = Instant.now();
//        Instant start = end.minusSeconds(24 * 60 * 60); // ✅ 최근 24시간
//
//        System.out.println("[DEBUG] 요청 범위: " + start + " ~ " + end);
//
//        try {
//            // CloudWatch 요청 생성
//            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
//                    .namespace("AWS/EC2") // EC2 메트릭 네임스페이스
//                    .metricName("CPUUtilization") // CPU 사용률
//                    .dimensions(Dimension.builder()
//                            .name("InstanceId")
//                            .value(instanceId)
//                            .build())
//                    .startTime(start)
//                    .endTime(end)
//                    .period(300) // 5분 간격 (300초)
//                    .statistics(Statistic.AVERAGE)
//                    .build();
//
//            // CloudWatch 호출
//            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
//
//            // 결과가 없을 경우
//            if (response.datapoints().isEmpty()) {
//                System.out.println("⚠️ CloudWatch에서 메트릭 데이터를 반환하지 않았습니다.");
//                return List.of();
//            }
//
//            // 결과 변환
//            List<Map<String, Object>> result = response.datapoints().stream()
//                    .map(dp -> {
//                        Map<String, Object> map = new HashMap<>();
//                        map.put("time", dp.timestamp().toString());
//                        map.put("value", dp.average());
//                        return map;
//                    })
//                    .sorted(Comparator.comparing(m -> (String) m.get("time")))
//                    .collect(Collectors.toList());
//
//            System.out.println("[INFO] 수집된 데이터 개수: " + result.size());
//            result.forEach(r -> System.out.println("⏱️ " + r.get("time") + " → " + r.get("value") + "%"));
//            return result;
//
//        } catch (CloudWatchException e) {
//            System.err.println("❌ CloudWatch 호출 실패: " + e.awsErrorDetails().errorMessage());
//            throw e;
//        }
//    }
//
//    private List<MetricPoint> toPoints(List<Datapoint> dps, boolean sortAsc) {
//        List<MetricPoint> list = dps.stream()
//                .map(dp -> new MetricPoint(dp.timestamp().toString(),
//                        dp.average() != null ? dp.average()
//                                : dp.sum() != null ? dp.sum()
//                                : dp.maximum() != null ? dp.maximum()
//                                : dp.minimum() != null ? dp.minimum()
//                                : null))
//                .filter(p -> p.getValue() != null)
//                .collect(Collectors.toList());
//        list.sort(Comparator.comparing(MetricPoint::getTime));
//        if (!sortAsc) Collections.reverse(list);
//        return list;
//    }
//
//    /** 공통 헬퍼(getMetricStatistics) */
//    private List<MetricPoint> getStats(String namespace, String metricName,
//                                       List<Dimension> dims, int periodSec,
//                                       Statistic stat, Instant start, Instant end) {
//        GetMetricStatisticsRequest req = GetMetricStatisticsRequest.builder()
//                .namespace(namespace)
//                .metricName(metricName)
//                .dimensions(dims)
//                .startTime(start)
//                .endTime(end)
//                .period(periodSec)
//                .statistics(stat)
//                .build();
//        GetMetricStatisticsResponse res = cw.getMetricStatistics(req);
//        return toPoints(res.datapoints(), true);
//    }
//
//    // ====== EC2 ======
//    public List<MetricPoint> ec2Cpu(String instanceId) {
//        Instant end = Instant.now();
//        Instant start = end.minusSeconds(24 * 60 * 60);
//        return getStats(
//                "AWS/EC2", "CPUUtilization",
//                List.of(Dimension.builder().name("InstanceId").value(instanceId).build()),
//                300, Statistic.AVERAGE, start, end);
//    }
//
//    public List<MetricPoint> ec2NetworkIn(String instanceId) {
//        Instant end = Instant.now();
//        Instant start = end.minusSeconds(24 * 60 * 60);
//        return getStats(
//                "AWS/EC2", "NetworkIn",
//                List.of(Dimension.builder().name("InstanceId").value(instanceId).build()),
//                300, Statistic.SUM, start, end);
//    }
//
//    public List<MetricPoint> ec2NetworkOut(String instanceId) {
//        Instant end = Instant.now();
//        Instant start = end.minusSeconds(24 * 60 * 60);
//        return getStats(
//                "AWS/EC2", "NetworkOut",
//                List.of(Dimension.builder().name("InstanceId").value(instanceId).build()),
//                300, Statistic.SUM, start, end);
//    }
//
//    // ====== RDS ======
//    public List<MetricPoint> rdsCpu(String dbInstanceId) {
//        Instant end = Instant.now();
//        Instant start = end.minusSeconds(24 * 60 * 60);
//        return getStats(
//                "AWS/RDS", "CPUUtilization",
//                List.of(Dimension.builder().name("DBInstanceIdentifier").value(dbInstanceId).build()),
//                300, Statistic.AVERAGE, start, end);
//    }
//
//    // ====== S3 ======
//    public List<MetricPoint> s3BucketSize(String bucketName, String storageType) {
//        // S3 스토리지 메트릭은 일 단위 집계/지연
//        Instant end = Instant.now();
//        Instant start = end.minusSeconds(7L * 24 * 60 * 60);
//        return getStats(
//                "AWS/S3", "BucketSizeBytes",
//                List.of(
//                        Dimension.builder().name("BucketName").value(bucketName).build(),
//                        Dimension.builder().name("StorageType").value(storageType).build() // e.g., StandardStorage
//                ),
//                86400, Statistic.AVERAGE, start, end);
//    }
//}
