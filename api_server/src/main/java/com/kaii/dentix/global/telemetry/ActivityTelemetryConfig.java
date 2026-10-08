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
    private static String producer(Environment env) {
      String id=env.getRequiredProperty("ACTIVITY_PRODUCER_ID");
      if(!id.equals("ec2-instance"))return id;
      try {
        String token=metadata("/api/token","PUT",null);
        String instance=metadata("/meta-data/instance-id","GET",token);
        if(!instance.matches("i-[0-9a-f]{8,17}"))throw new IllegalStateException("invalid_activity_producer");
        return instance;
      }catch(Exception ignored){throw new IllegalStateException("activity_producer_unavailable");}
    }
    private static String metadata(String path,String method,String token) throws Exception {
      var connection=(java.net.HttpURLConnection)java.net.URI.create("http://169.254.169.254/latest"+path).toURL().openConnection();
      connection.setConnectTimeout(100);connection.setReadTimeout(100);connection.setRequestMethod(method);
      connection.setInstanceFollowRedirects(false);
      if(token==null)connection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds","60");
      else connection.setRequestProperty("X-aws-ec2-metadata-token",token);
      try {
        if(connection.getResponseCode()!=200)throw new IllegalStateException("metadata_unavailable");
        try(var input=connection.getInputStream()){return new String(input.readNBytes(1024),StandardCharsets.UTF_8).trim();}
      }finally{connection.disconnect();}
    }
    AwsSink(Environment env) {
      table=env.getRequiredProperty("ACTIVITY_RAW_TABLE");producer=producer(env);
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
    }
    private void put(Map<String,AttributeValue> item,String field,long at) {
      try{dynamo.putItem(r->r.tableName(table).item(item).conditionExpression("attribute_not_exists(pk) OR #at < :at").expressionAttributeNames(Map.of("#at",field)).expressionAttributeValues(Map.of(":at",n(at))));}
      catch(ConditionalCheckFailedException stale){ /* A duplicate or older observation cannot rewind activity. */ }
    }
    static AttributeValue s(String v){return AttributeValue.builder().s(v).build();}
    static AttributeValue n(long v){return AttributeValue.builder().n(Long.toString(v)).build();}
  }
}
