package com.kaii.dentix.global.telemetry;

import java.time.*;
import java.util.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class BusinessUsageTelemetry {
  public record Event(String type,String kind,String rawId,Instant at) {}
  private final boolean enabled;
  private final ActivityTelemetry activity;
  private final Clock clock;
  public BusinessUsageTelemetry(boolean enabled,ActivityTelemetry activity,Clock clock) {
    this.enabled=enabled;this.activity=activity;this.clock=clock;
  }
  public void institution(String verifiedOrganization,Instant at) {
    if(!enabled)return;
    submit(new Event(verifiedOrganization==null?"unattributed":"institution",null,verifiedOrganization,at));
  }
  public void completed(String kind,String persistedId,Instant completedAt) {
    if(!enabled)return;
    try {
      if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.isSynchronizationActive()) {activity.reportCollectionFailure();return;}
      var event=new Event("completion",kind,persistedId,completedAt);
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit(){submit(event);}
      });
    }catch(RuntimeException ignored){activity.reportCollectionFailure();}
  }
  private void submit(Event event) {
    try {
      if(event.at()==null||event.at().isAfter(clock.instant().plusSeconds(60))||!event.at().plusSeconds(72*3600).isAfter(clock.instant()))throw new IllegalArgumentException();
      if(!event.type().equals("unattributed")&&(event.rawId()==null||event.rawId().isBlank()||event.rawId().length()>1024))throw new IllegalArgumentException();
      if(event.type().equals("completion")&&!Set.of("oral","gingivitis","plaque").contains(event.kind()))throw new IllegalArgumentException();
      activity.recordBusiness(event);
    }catch(RuntimeException ignored){activity.reportCollectionFailure();}
  }
  static Map<String,Object> event(String service,Event event,byte[] dayKey,String keyVersion) throws Exception {
    String day=ActivityTelemetry.day(event.at());
    var row=new HashMap<String,Object>();
    row.put("version",1);row.put("serviceId",service);row.put("dayKey",day);row.put("expiresAt",event.at().getEpochSecond()+72*3600);
    if(event.type().equals("unattributed")) {
      row.put("pk","BUSINESS_META#v1#"+service+"#"+day);row.put("sk","UNATTRIBUTED#"+event.at().toEpochMilli()+"#"+UUID.randomUUID());
      row.put("at",event.at().toEpochMilli());row.put("unattributedRequests",1);return row;
    }
    row.put("keyVersion",keyVersion);
    boolean done=event.type().equals("completion");
    String hash=ActivityTelemetry.hash(dayKey,done?List.of(1,service,"completion",event.kind(),event.rawId()):List.of(1,service,"institution",day,event.rawId()));
    row.put("pk","BUSINESS_"+(done?"DONE":"ORG")+"#v1#"+service+"#"+day);row.put("sk",done?event.kind()+"#"+hash:hash);
    row.put(done?"resultHash":"organizationHash",hash);row.put(done?"completedAt":"lastSeenAt",event.at().toEpochMilli());
    if(done)row.put("kind",event.kind());
    return row;
  }
}
