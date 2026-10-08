package com.kaii.dentix.global.telemetry;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Configuration
public class ActivityTelemetryConfig {
  @Bean BusinessUsageTelemetry businessUsageTelemetry(Environment env,ActivityTelemetry activity) {
    return new BusinessUsageTelemetry(env.getProperty("BUSINESS_USAGE_ENABLED",Boolean.class,false),activity,Clock.systemUTC());
  }
  @Bean(destroyMethod="close") ActivityTelemetry activityTelemetry(Environment env) {
    boolean enabled=env.getProperty("ACTIVITY_ENABLED",Boolean.class,false);
    try {return new ActivityTelemetry(enabled,enabled?new AwsSink(env):null,Clock.systemUTC(),true);}
    catch(RuntimeException ignored) {
      org.slf4j.LoggerFactory.getLogger(ActivityTelemetryConfig.class).warn("Activity telemetry configuration unavailable; collection disabled");
      return new ActivityTelemetry(false,null,Clock.systemUTC(),false);
    }
  }
  static final class AwsSink implements ActivityTelemetry.Sink {
    private final String service="soh",table,producer,daySecret,windowSecret;
    private final DynamoDbClient dynamo;
    private final SecretsManagerClient secrets;
    private String cachedDay,version;
    private byte[] dayKey,windowKey;
    private final boolean businessEnabled;
    AwsSink(Environment env) {
      businessEnabled=env.getProperty("BUSINESS_USAGE_ENABLED",Boolean.class,false);
      table=env.getRequiredProperty("ACTIVITY_RAW_TABLE");producer=env.getRequiredProperty("ACTIVITY_PRODUCER_ID");
      daySecret=env.getRequiredProperty("ACTIVITY_DAY_SECRET_ID");windowSecret=env.getRequiredProperty("ACTIVITY_WINDOW_SECRET_ID");
      var region=Region.of(env.getProperty("ACTIVITY_REGION","ap-northeast-2"));
      var limits=ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(100)).apiCallAttemptTimeout(Duration.ofMillis(100)).retryPolicy(RetryPolicy.none()).build();
      dynamo=DynamoDbClient.builder().region(region).overrideConfiguration(limits).httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofMillis(100)).socketTimeout(Duration.ofMillis(100))).build();
      secrets=SecretsManagerClient.builder().region(region).overrideConfiguration(limits).httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofMillis(100)).socketTimeout(Duration.ofMillis(100))).build();
    }
    private synchronized void keys(Instant at) {
      String day=ActivityTelemetry.day(at);if(day.equals(cachedDay))return;
      var key=Map.of("pk",s("HEARTBEAT#"+service+"#keys"),"sk",s("DAY#"+day));
      var pinned=dynamo.getItem(r->r.tableName(table).key(key).consistentRead(true)).item();
      String pinnedVersion=pinned.get("keyVersion")==null?null:pinned.get("keyVersion").s();
      GetSecretValueResponse value=secrets.getSecretValue(r->r.secretId(daySecret).versionId(pinnedVersion));
      if(pinnedVersion==null) {
        var pin=new HashMap<>(key);pin.put("keyVersion",s(value.versionId()));pin.put("expiresAt",n(at.getEpochSecond()+72*3600));
        try{dynamo.putItem(r->r.tableName(table).item(pin).conditionExpression("attribute_not_exists(pk)"));}
        catch(ConditionalCheckFailedException race) {
          String winner=dynamo.getItem(r->r.tableName(table).key(key).consistentRead(true)).item().get("keyVersion").s();
          value=secrets.getSecretValue(r->r.secretId(daySecret).versionId(winner));
        }
      }
      if(windowKey==null)windowKey=material(secrets.getSecretValue(r->r.secretId(windowSecret)).secretString());
      dayKey=material(value.secretString());version=value.versionId();cachedDay=day;
    }
    private static byte[] material(String value) {
      if(value==null||value.getBytes(StandardCharsets.UTF_8).length!=32)throw new IllegalArgumentException("invalid_activity_key");
      return value.getBytes(StandardCharsets.UTF_8);
    }
    @Override public void write(ActivityTelemetry.Identity identity) throws Exception {
      Map<String,Object> row;
      synchronized(this){keys(identity.at());row=ActivityTelemetry.event(service,identity,dayKey,windowKey,version);}
      var item=new HashMap<String,AttributeValue>();row.forEach((k,v)->item.put(k,v instanceof Number number?n(number.longValue()):s((String)v)));
      put(item,"lastSeenAt",identity.at().toEpochMilli());
    }
    @Override public void heartbeat(long at,long errorAt) {
      keys(Instant.ofEpochMilli(at));
      put(Map.of("pk",s("HEARTBEAT#"+service+"#producers"),"sk",s(producer),"producer",s(producer),"at",n(at),"errorAt",n(errorAt),"expiresAt",n(at/1000+72*3600)),"at",at);
      if(businessEnabled) {
        long minute=at/60000*60000;String day=ActivityTelemetry.day(Instant.ofEpochMilli(at));
        putOnce(Map.of("pk",s("BUSINESS_META#v1#"+service+"#"+day),"sk",s("PRODUCER#"+minute+"#"+producer),"version",n(1),"serviceId",s(service),"dayKey",s(day),"producer",s(producer),"at",n(minute),"expiresAt",n(at/1000+72*3600)));
      }
    }
    @Override public void business(BusinessUsageTelemetry.Event event) throws Exception {
      if(!businessEnabled)throw new IllegalStateException("business_disabled");
      Map<String,Object> row;
      synchronized(this){keys(event.at());row=BusinessUsageTelemetry.event(service,event,dayKey,version);}
      var item=new HashMap<String,AttributeValue>();row.forEach((k,v)->item.put(k,v instanceof Number number?n(number.longValue()):s((String)v)));
      if(event.type().equals("institution"))put(item,"lastSeenAt",event.at().toEpochMilli());else putOnce(item);
    }
    private void putOnce(Map<String,AttributeValue> item) {
      try{dynamo.putItem(r->r.tableName(table).item(item).conditionExpression("attribute_not_exists(pk)"));}
      catch(ConditionalCheckFailedException duplicate){ /* Persisted result identity is stable on retry. */ }
    }
    private void put(Map<String,AttributeValue> item,String field,long at) {
      try{dynamo.putItem(r->r.tableName(table).item(item).conditionExpression("attribute_not_exists(pk) OR #at < :at").expressionAttributeNames(Map.of("#at",field)).expressionAttributeValues(Map.of(":at",n(at))));}
      catch(ConditionalCheckFailedException stale){ /* A duplicate or older observation cannot rewind activity. */ }
    }
    static AttributeValue s(String v){return AttributeValue.builder().s(v).build();}
    static AttributeValue n(long v){return AttributeValue.builder().n(Long.toString(v)).build();}
  }
}
