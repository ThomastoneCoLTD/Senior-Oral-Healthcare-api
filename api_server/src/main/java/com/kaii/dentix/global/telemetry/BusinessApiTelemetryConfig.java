package com.kaii.dentix.global.telemetry;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

@Configuration
public class BusinessApiTelemetryConfig {
  @Bean(destroyMethod = "close")
  BusinessApiTelemetry businessApiTelemetry(Environment env, ObjectProvider<ActivityTelemetry> activity) {
    boolean enabled = env.getProperty("BUSINESS_API_ENABLED", Boolean.class, false);
    Runnable failure = () -> { var writer = activity.getIfAvailable(); if (writer != null) writer.reportCollectionFailure(); };
    if (!enabled) return new BusinessApiTelemetry(false, null, Clock.systemUTC(), failure, false);
    try {
      var limits = ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofSeconds(1)).apiCallAttemptTimeout(Duration.ofSeconds(1)).retryPolicy(RetryPolicy.none()).build();
      var client = CloudWatchClient.builder().region(Region.of(env.getProperty("ACTIVITY_REGION", "ap-northeast-2"))).overrideConfiguration(limits)
          .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(1)).socketTimeout(Duration.ofSeconds(1))).build();
      return new BusinessApiTelemetry(true, points -> client.putMetricData(r -> r.namespace("Denti/BusinessApi").metricData(metricData(points))), Clock.systemUTC(), failure, true);
    } catch (RuntimeException ignored) {
      org.slf4j.LoggerFactory.getLogger(BusinessApiTelemetryConfig.class).warn("Business API telemetry configuration unavailable; collection disabled");
      return new BusinessApiTelemetry(false, null, Clock.systemUTC(), failure, false);
    }
  }

  static List<MetricDatum> metricData(List<BusinessApiTelemetry.Point> points) {
    var result = new ArrayList<MetricDatum>();
    for (var point : points) {
      var dimensions = new ArrayList<Dimension>();
      dimensions.add(Dimension.builder().name("ServiceId").value("soh").build());
      var metric = MetricDatum.builder().metricName(point.name()).timestamp(point.at());
      if (point.status() != null) {
        dimensions.add(Dimension.builder().name("StatusClass").value(point.status()).build());
        metric.unit(StandardUnit.MILLISECONDS).statisticValues(StatisticSet.builder().sampleCount((double) point.count()).sum(point.sum()).minimum(point.min()).maximum(point.max()).build());
      } else metric.unit(StandardUnit.COUNT).value(point.sum());
      result.add(metric.dimensions(dimensions).build());
    }
    return result;
  }
}
