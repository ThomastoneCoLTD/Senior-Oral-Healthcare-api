//package com.kaii.dentix.global.common.aws.controller;
//
//import com.kaii.dentix.global.common.aws.CloudWatchService;
//import com.kaii.dentix.global.common.aws.dto.MetricPoint;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//import software.amazon.awssdk.services.cloudwatch.model.*;
//
//import java.time.Instant;
//import java.util.*;
//@RestController
//@RequestMapping("/api/aws/metrics")
//@RequiredArgsConstructor
//public class CloudWatchController {
//
//    private final CloudWatchService service;
//
//    // EC2
//    @GetMapping("/ec2/cpu")
//    public List<MetricPoint> ec2Cpu(@RequestParam String instanceId) {
//        return service.ec2Cpu(instanceId);
//    }
//
//    @GetMapping("/ec2/network-in")
//    public List<MetricPoint> ec2NetworkIn(@RequestParam String instanceId) {
//        return service.ec2NetworkIn(instanceId);
//    }
//
//    @GetMapping("/ec2/network-out")
//    public List<MetricPoint> ec2NetworkOut(@RequestParam String instanceId) {
//        return service.ec2NetworkOut(instanceId);
//    }
//
//    // RDS
//    @GetMapping("/rds/cpu")
//    public List<MetricPoint> rdsCpu(@RequestParam String dbInstanceId) {
//        return service.rdsCpu(dbInstanceId);
//    }
//
//    // S3
//    @GetMapping("/s3/bucket-size")
//    public List<MetricPoint> s3BucketSize(@RequestParam String bucketName,
//                                          @RequestParam(defaultValue = "StandardStorage") String storageType) {
//        return service.s3BucketSize(bucketName, storageType);
//    }
//}