package com.kaii.dentix.global.telemetry;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Request threads enqueue numbers only; one worker publishes closed-minute statistics. */
public final class BusinessApiTelemetry implements AutoCloseable {
  public record Point(String name, String status, Instant at, long count, double sum, double min, double max) {}
  interface Sink { void publish(List<Point> points) throws Exception; }
  private record Sample(int status, double duration, Instant at) {}
  private record Bucket(long minute, int status) {}
  private static final class Statistics {
    long count; double sum; double min = Double.POSITIVE_INFINITY; double max;
    void add(double value) { count++; sum += value; min = Math.min(min, value); max = Math.max(max, value); }
  }

  private final boolean enabled;
  private final Sink sink;
  private final Clock clock;
  private final Runnable failure;
  private final long startedAt;
  private final ArrayBlockingQueue<Sample> queue = new ArrayBlockingQueue<>(256);
  private final Map<Bucket, Statistics> buckets = new HashMap<>();
  private final AtomicLong dropped = new AtomicLong(), publishFailed = new AtomicLong(), unsupported = new AtomicLong();
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> { var thread = new Thread(r, "business-api-telemetry"); thread.setDaemon(true); return thread; });
  private long lastMinute;

  BusinessApiTelemetry(boolean enabled, Sink sink, Clock clock, Runnable failure, boolean start) {
    this.enabled = enabled; this.sink = sink; this.clock = clock; this.failure = failure;
    startedAt = clock.millis(); lastMinute = Math.floorDiv(startedAt, 60000);
    if (enabled && start) executor.scheduleWithFixedDelay(this::flush, 1, 1, TimeUnit.SECONDS);
  }

  public void record(int status, double durationMs, Instant at) {
    if (!enabled) return;
    if (status < 200 || status > 599 || !Double.isFinite(durationMs) || durationMs < 0 || at == null || at.toEpochMilli() < clock.millis() - 300000 || at.toEpochMilli() > clock.millis() + 60000 || !queue.offer(new Sample(status, durationMs, at))) {
      dropped.incrementAndGet(); markFailure();
    }
  }

  public void unsupportedAsync() {
    if (enabled) { unsupported.incrementAndGet(); markFailure(); }
  }

  private void markFailure() {
    try { failure.run(); } catch (RuntimeException ignored) { /* Keep instrumentation outside the business response. */ }
  }

  public synchronized void flush() {
    if (!enabled) return;
    for (int i = 0; i < 256; i++) {
      var sample = queue.poll();
      if (sample == null) break;
      var key = new Bucket(Math.floorDiv(sample.at().toEpochMilli(), 60000), sample.status() / 100);
      if (!buckets.containsKey(key) && buckets.size() >= 32) { dropped.incrementAndGet(); markFailure(); continue; }
      buckets.computeIfAbsent(key, ignored -> new Statistics()).add(sample.duration());
    }
    long minute = Math.floorDiv(clock.millis(), 60000);
    if (minute <= lastMinute) return;
    var points = new ArrayList<Point>();
    var iterator = buckets.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (entry.getKey().minute() >= minute) continue;
      var key = entry.getKey(); var value = entry.getValue();
      points.add(new Point("Duration", key.status() + "xx", Instant.ofEpochMilli(key.minute() * 60000), value.count, value.sum, value.min, value.max));
      iterator.remove();
    }
    var at = Instant.ofEpochMilli((minute - 1) * 60000);
    counter(points, "Dropped", at, dropped.getAndSet(0));
    counter(points, "PublishFailed", at, publishFailed.getAndSet(0));
    counter(points, "UnsupportedAsync", at, unsupported.getAndSet(0));
    // Never fabricate missed historical heartbeats after a paused process resumes.
    if (startedAt <= at.toEpochMilli() && minute == lastMinute + 1) counter(points, "PublishedIntervals", at, 1);
    lastMinute = minute;
    if (points.isEmpty()) return;
    try { sink.publish(List.copyOf(points)); }
    catch (Exception ignored) {
      // Retrying a timeout can double CloudWatch sums. Preserve the loss as quality evidence.
      publishFailed.incrementAndGet(); markFailure();
    }
  }

  private static void counter(List<Point> points, String name, Instant at, long value) {
    if (value > 0) points.add(new Point(name, null, at, 1, value, value, value));
  }

  @Override public void close() { executor.shutdownNow(); queue.clear(); }
}
