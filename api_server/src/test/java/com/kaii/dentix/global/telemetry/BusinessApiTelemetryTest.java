package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BusinessApiTelemetryTest {
  static final Instant NOW = Instant.parse("2026-10-08T03:00:00Z");
  static class MutableClock extends Clock {
    Instant at = NOW;
    public ZoneId getZone() { return ZoneOffset.UTC; }
    public Clock withZone(ZoneId zone) { return this; }
    public Instant instant() { return at; }
  }
  static class Sink implements BusinessApiTelemetry.Sink {
    final List<BusinessApiTelemetry.Point> points = new ArrayList<>();
    boolean fail;
    public void publish(List<BusinessApiTelemetry.Point> batch) {
      if (fail) throw new IllegalStateException("fixture transport");
      points.addAll(batch);
    }
  }
  @Test void aggregatesByCompletionMinuteAndStatusWithoutTransportTime() {
    var sink = new Sink(); var clock = new MutableClock();
    try (var writer = new BusinessApiTelemetry(true, sink, clock, () -> {}, false)) {
      writer.record(200, 25, NOW); writer.record(200, 75, NOW.plusSeconds(10)); writer.record(422, 10, NOW);
      writer.flush(); assertThat(sink.points).isEmpty();
      clock.at = NOW.plusSeconds(60); writer.flush();
      var measured = sink.points.stream().filter(p -> p.name().equals("Duration") && p.status().equals("2xx")).findFirst().orElseThrow();
      assertThat(measured.count()).isEqualTo(2); assertThat(measured.sum()).isEqualTo(100);
      assertThat(measured.min()).isEqualTo(25); assertThat(measured.max()).isEqualTo(75);
      assertThat(measured.at()).isEqualTo(NOW);
      var datum = BusinessApiTelemetryConfig.metricData(List.of(measured)).get(0);
      assertThat(datum.statisticValues().sampleCount()).isEqualTo(2);
      assertThat(datum.statisticValues().sum()).isEqualTo(100);
      assertThat(datum.dimensions()).extracting(d -> d.name() + "=" + d.value()).containsExactly("ServiceId=soh", "StatusClass=2xx");
      assertThat(sink.points.stream().filter(p -> p.name().equals("PublishedIntervals")).count()).isEqualTo(1);
      int sent = sink.points.size(); writer.flush(); assertThat(sink.points).hasSize(sent);
    }
  }
  @Test void overflowAndFailedPublishArePartialInsteadOfThrowingOrRetryingCounts() {
    var sink = new Sink(); var clock = new MutableClock(); var failures = new AtomicInteger();
    try (var writer = new BusinessApiTelemetry(true, sink, clock, failures::incrementAndGet, false)) {
      for (int i = 0; i < 257; i++) writer.record(200, 1, NOW);
      clock.at = NOW.plusSeconds(60); writer.flush();
      assertThat(sink.points.stream().filter(p -> p.name().equals("Duration")).mapToLong(BusinessApiTelemetry.Point::count).sum()).isEqualTo(256);
      assertThat(sink.points.stream().filter(p -> p.name().equals("Dropped")).mapToDouble(BusinessApiTelemetry.Point::sum).sum()).isEqualTo(1);
      sink.fail = true; writer.record(500, 10, clock.at);
      clock.at = NOW.plusSeconds(120); assertThatCode(writer::flush).doesNotThrowAnyException();
      sink.fail = false; clock.at = NOW.plusSeconds(180); writer.flush();
      assertThat(sink.points.stream().filter(p -> p.name().equals("PublishFailed")).mapToDouble(BusinessApiTelemetry.Point::sum).sum()).isEqualTo(1);
      assertThat(sink.points.stream().filter(p -> "5xx".equals(p.status()))).isEmpty();
      assertThat(failures.get()).isGreaterThanOrEqualTo(2);
    }
  }
  @Test void concurrentOffersPreserveCountAndMissingIntervalsAreNotBackfilled() throws Exception {
    var sink = new Sink(); var clock = new MutableClock();
    try (var writer = new BusinessApiTelemetry(true, sink, clock, () -> {}, false)) {
      var pool = Executors.newFixedThreadPool(4);
      try {
        var jobs = new ArrayList<Future<?>>();
        for (int i = 0; i < 100; i++) {
          jobs.add(pool.submit(() -> writer.record(200, 2, NOW)));
          jobs.add(pool.submit(writer::flush));
        }
        for (var job : jobs) job.get();
      } finally { pool.shutdownNow(); }
      clock.at = NOW.plusSeconds(180); writer.flush();
      assertThat(sink.points.stream().filter(p -> p.name().equals("Duration")).mapToLong(BusinessApiTelemetry.Point::count).sum()).isEqualTo(100);
      assertThat(sink.points.stream().filter(p -> p.name().equals("PublishedIntervals")).count()).isLessThanOrEqualTo(1);
    }
  }
}
