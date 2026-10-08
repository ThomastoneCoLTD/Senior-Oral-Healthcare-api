package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.*;
import org.springframework.transaction.TransactionDefinition;

class BusinessUsageTelemetryTest {
  static final Instant AT=Instant.parse("2026-10-08T14:59:59Z");
  static class Sink implements ActivityTelemetry.Sink {
    final List<BusinessUsageTelemetry.Event> rows=new ArrayList<>();
    public void write(ActivityTelemetry.Identity identity) {}
    public void heartbeat(long at,long errorAt) {}
    public void business(BusinessUsageTelemetry.Event event) { rows.add(event); }
  }
  static class Manager extends AbstractPlatformTransactionManager {
    protected Object doGetTransaction(){return new Object();}
    protected void doBegin(Object transaction,TransactionDefinition definition){}
    protected void doCommit(DefaultTransactionStatus status){}
    protected void doRollback(DefaultTransactionStatus status){}
  }
  @Test void onlyCommittedWorkIsQueuedAndRollbacksOrNoTransactionRemainZero() {
    var sink=new Sink();
    try(var activity=new ActivityTelemetry(true,sink,Clock.fixed(AT,ZoneOffset.UTC),false)) {
      var usage=new BusinessUsageTelemetry(true,activity,Clock.fixed(AT,ZoneOffset.UTC));
      usage.completed("gingivitis","1",AT); activity.flush(); assertThat(sink.rows).isEmpty();
      var tx=new TransactionTemplate(new Manager());
      assertThatThrownBy(()->tx.execute(status->{usage.completed("gingivitis","2",AT);throw new IllegalStateException("later authorization or billing failed");})).isInstanceOf(IllegalStateException.class);
      activity.flush();assertThat(sink.rows).isEmpty();
      tx.execute(status->{usage.completed("gingivitis","3",AT);assertThat(activity.pending()).isZero();return null;});
      activity.flush();assertThat(sink.rows).hasSize(1);assertThat(sink.rows.get(0).rawId()).isEqualTo("3");
    }
  }
  @Test void sameResultAndOccurrenceKeepHashAcrossMidnightAndNewResultIsDistinct() throws Exception {
    byte[] key=new byte[32];Arrays.fill(key,(byte)7);
    var event=new BusinessUsageTelemetry.Event("completion","gingivitis","fixture-1",AT);
    var row=BusinessUsageTelemetry.event("soh",event,key,"fixture-version");
    assertThat(row).isEqualTo(BusinessUsageTelemetry.event("soh",event,key,"fixture-version"));
    assertThat(row.get("pk")).isEqualTo("BUSINESS_DONE#v1#soh#2026-10-08");
    assertThat(row.get("sk")).isNotEqualTo(BusinessUsageTelemetry.event("soh",new BusinessUsageTelemetry.Event("completion","gingivitis","fixture-2",AT),key,"fixture-version").get("sk"));
    assertThat(row.values()).doesNotContain("fixture-1");
  }
  @Test void institutionAndUsageShareBoundedQueueAndMissingInstitutionIsExplicit() {
    var sink=new Sink();
    try(var activity=new ActivityTelemetry(true,sink,Clock.fixed(AT,ZoneOffset.UTC),false)) {
      var usage=new BusinessUsageTelemetry(true,activity,Clock.fixed(AT,ZoneOffset.UTC));
      usage.institution("verified-org",AT);usage.institution(null,AT);activity.flush();
      assertThat(sink.rows).extracting(BusinessUsageTelemetry.Event::type).containsExactly("institution","unattributed");
      for(int i=0;i<256;i++)activity.record("account","fixture","user",AT);
      assertThatCode(()->usage.institution("overflow",AT)).doesNotThrowAnyException();
      assertThat(activity.pending()).isEqualTo(256);
    }
  }
}
