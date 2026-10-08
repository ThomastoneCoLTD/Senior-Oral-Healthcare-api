package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ActivityTelemetryTest {
  static final Instant NOW = Instant.ofEpochMilli(1791471570000L);
  static class Sink implements ActivityTelemetry.Sink {
    List<ActivityTelemetry.Identity> events = new ArrayList<>();
    long errorAt;
    boolean fail;
    public void write(ActivityTelemetry.Identity event) { if(fail) throw new IllegalStateException(); events.add(event); }
    public void heartbeat(long at, long error) { errorAt=error; }
  }
  @Test void matchesSharedNodeFixtureAndSeparatesDaysAndCohorts() throws Exception {
    byte[] day=new byte[32],window=new byte[32]; Arrays.fill(day,(byte)1); Arrays.fill(window,(byte)2);
    var identity=new ActivityTelemetry.Identity("embed","[\"fixture-partner\",\"fixture-user\"]","user",NOW);
    var row=ActivityTelemetry.event("caresync",identity,day,window,"fixture-v1");
    assertThat(row.get("subjectHash")).isEqualTo("319cfd46783ecb60b0d2e0c111e0e94d5fe5911e30f39e473eb1a6fd2783af92");
    assertThat(row.get("windowHash")).isEqualTo("a2167ae4eb49b9243e7ce454a0debb5ed5ae86efc55a368a9e4656c751621614");
    assertThat(row.get("expiresAt")).isEqualTo(1791730770L);
    assertThat(row.values()).doesNotContain(identity.principal());
    var tomorrow=ActivityTelemetry.event("caresync",new ActivityTelemetry.Identity("embed",identity.principal(),"admin",NOW.plusSeconds(86400)),day,window,"fixture-v1");
    assertThat(tomorrow.get("subjectHash")).isNotEqualTo(row.get("subjectHash"));
    assertThat(tomorrow.get("windowHash")).isEqualTo(row.get("windowHash"));
    assertThat(ActivityTelemetry.event("caresync",new ActivityTelemetry.Identity("admin",identity.principal(),"admin",NOW),day,window,"fixture-v1").get("subjectHash")).isNotEqualTo(row.get("subjectHash"));
  }
  @Test void boundedQueueAndFailedWritesDoNotThrowIntoBusinessRequests() {
    var sink=new Sink();
    try(var writer=new ActivityTelemetry(true,sink,Clock.fixed(NOW,ZoneOffset.UTC),false)) {
      for(int i=0;i<257;i++) writer.record("account","fixture","user",NOW);
      assertThat(writer.pending()).isEqualTo(256);
      writer.heartbeat(); assertThat(sink.errorAt).isEqualTo(NOW.toEpochMilli());
      sink.fail=true;
      assertThatCode(writer::flush).doesNotThrowAnyException();
      assertThat(writer.pending()).isZero();
      writer.heartbeat(); assertThat(sink.errorAt).isEqualTo(NOW.toEpochMilli());
    }
  }
  @Test void disabledWriterIsSilentAndValidEventsAreAsynchronous() {
    var sink=new Sink();
    try(var writer=new ActivityTelemetry(false,sink,Clock.fixed(NOW,ZoneOffset.UTC),false)) {
      writer.record("account","fixture","user",NOW);writer.flush();assertThat(sink.events).isEmpty();
    }
    try(var writer=new ActivityTelemetry(true,sink,Clock.fixed(NOW,ZoneOffset.UTC),false)) {
      writer.record("account","fixture","user",NOW); assertThat(sink.events).isEmpty();
      writer.flush();assertThat(sink.events).hasSize(1);
    }
  }
}
