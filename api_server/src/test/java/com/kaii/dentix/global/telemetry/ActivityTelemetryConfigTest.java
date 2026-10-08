package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ActivityTelemetryConfigTest {
  @Test void missingTelemetrySettingsCannotPreventBusinessApplicationStartup() {
    assertThatCode(()->{
      try(var writer=new ActivityTelemetryConfig().activityTelemetry(new MockEnvironment().withProperty("ACTIVITY_ENABLED","true"))){
        writer.heartbeat();
      }
    }).doesNotThrowAnyException();
  }
}
