package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BusinessUsageContextTest {
  @Test void liveInstitutionIsRequestScopedAndNoContextRetainsIdentity() {
    BusinessUsageContext.capture(()->"ignored");assertThat(BusinessUsageContext.organization()).isNull();
    try(var request=BusinessUsageContext.open()) {
      BusinessUsageContext.capture(()->"old-organization");assertThat(BusinessUsageContext.organization()).isEqualTo("old-organization");
      BusinessUsageContext.capture(()->"new-organization");assertThat(BusinessUsageContext.organization()).isEqualTo("new-organization");
      try(var nested=BusinessUsageContext.open()){assertThat(BusinessUsageContext.organization()).isNull();}
      assertThat(BusinessUsageContext.organization()).isEqualTo("new-organization");
      assertThatCode(()->BusinessUsageContext.capture(()->{throw new IllegalStateException("unavailable");})).doesNotThrowAnyException();
      assertThat(BusinessUsageContext.organization()).isNull();
    }
    assertThat(BusinessUsageContext.organization()).isNull();
  }
}
