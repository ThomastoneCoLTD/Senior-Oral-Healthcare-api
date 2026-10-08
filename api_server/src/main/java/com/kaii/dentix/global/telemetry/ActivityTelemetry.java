package com.kaii.dentix.global.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.LoggerFactory;

/** Request threads only enqueue; IAM/network failures never change the business response. */
public final class ActivityTelemetry implements AutoCloseable {
  public record Identity(String cohort,String principal,String role,Instant at) {}
  interface Sink {
    void write(Identity identity) throws Exception;
    void heartbeat(long at,long errorAt) throws Exception;
    default void business(BusinessUsageTelemetry.Event event) throws Exception { throw new IllegalStateException("business_writer_unavailable"); }
  }
  private final boolean enabled;
  private final Sink sink;
  private final Clock clock;
  private interface PendingWrite { void run() throws Exception; }
  private final ArrayBlockingQueue<PendingWrite> queue=new ArrayBlockingQueue<>(256);
  private final ScheduledThreadPoolExecutor executor=new ScheduledThreadPoolExecutor(2,r->{var t=new Thread(r,"activity-telemetry");t.setDaemon(true);return t;});
  private final AtomicLong errorAt=new AtomicLong();
  private final AtomicLong lastWarning=new AtomicLong();

  ActivityTelemetry(boolean enabled,Sink sink,Clock clock,boolean start) {
    this.enabled=enabled;this.sink=sink;this.clock=clock;
    if(enabled&&start) {
      executor.scheduleWithFixedDelay(this::flush,1,1,TimeUnit.SECONDS);
      executor.scheduleWithFixedDelay(this::heartbeat,0,60,TimeUnit.SECONDS);
    }
  }
  public void record(String cohort,String principal,String role,Instant at) {
    if(!enabled)return;
    if(principal==null||principal.isBlank()||principal.length()>1024||!Set.of("admin","user").contains(role)||!Set.of("account","admin","hospital","embed").contains(cohort)||at==null) { failed();return; }
    if(!queue.offer(()->sink.write(new Identity(cohort,principal,role,at))))failed();
  }
  void recordBusiness(BusinessUsageTelemetry.Event event) {
    if(enabled&&!queue.offer(()->sink.business(event)))failed();
  }
  int pending(){return queue.size();}
  void flush() {
    for(int n=0;n<256;n++) {
      var identity=queue.poll();if(identity==null)return;
      try {identity.run();}catch(Exception ignored){failed();}
    }
  }
  public void heartbeat() {
    if(!enabled)return;
    try {sink.heartbeat(clock.millis(),errorAt.get());}catch(Exception ignored){failed();}
  }
  private void failed() {
    long now=clock.millis();errorAt.accumulateAndGet(now,Math::max);
    long previous=lastWarning.get();
    if(now-previous>=60000&&lastWarning.compareAndSet(previous,now))
      LoggerFactory.getLogger(ActivityTelemetry.class).warn("Activity telemetry unavailable; collection marked incomplete");
  }
  public void reportCollectionFailure() { failed(); }
  static String day(Instant at){return at.atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString();}
  static Map<String,Object> event(String service,Identity identity,byte[] dayKey,byte[] windowKey,String version) throws Exception {
    String day=day(identity.at());
    var row=new HashMap<String,Object>();
    row.put("version",1);row.put("serviceId",service);row.put("dayKey",day);row.put("cohort",identity.cohort());row.put("role",identity.role());
    row.put("lastSeenAt",identity.at().toEpochMilli());row.put("keyVersion",version);row.put("expiresAt",identity.at().getEpochSecond()+72*3600);
    String subject=hash(dayKey,List.of(1,service,day,identity.cohort(),identity.principal()));
    row.put("subjectHash",subject);row.put("windowHash",hash(windowKey,List.of(1,service,identity.cohort(),identity.principal())));
    row.put("pk","ACTIVITY#"+service+"#"+day);row.put("sk","SUBJECT#"+subject);
    return row;
  }
  static String hash(byte[] key,List<Object> parts) throws Exception {
    if(key.length!=32)throw new IllegalArgumentException("invalid_activity_key");
    var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(new ObjectMapper().writeValueAsString(parts).getBytes(StandardCharsets.UTF_8)));
  }
  @Override public void close(){executor.shutdownNow();queue.clear();}
}
