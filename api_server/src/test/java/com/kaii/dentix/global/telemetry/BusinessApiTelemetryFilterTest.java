package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

class BusinessApiTelemetryFilterTest {
  public void business() {}
  @AfterEach void clear() { SecurityContextHolder.clearContext(); }
  void authenticate() {
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    var account = new User("1", "", authorities);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(account, null, authorities));
  }
  @Test void handledErrorsUseMonotonicClockAndThrownFailureIsFiveHundred() throws Exception {
    for (boolean throwing : List.of(false, true)) {
      authenticate(); var sink = new BusinessApiTelemetryTest.Sink(); var clock = new BusinessApiTelemetryTest.MutableClock(); var nanos = new AtomicLong();
      try (var api = new BusinessApiTelemetry(true, sink, clock, () -> {}, false)) {
        var request = new MockHttpServletRequest("GET", "/admin/user"); var response = new MockHttpServletResponse();
        request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, new HandlerMethod(this, getClass().getMethod("business")));
        var filter = new ActivityTelemetryFilter(() -> null, () -> api, nanos::get, clock);
        jakarta.servlet.FilterChain chain = (req, res) -> { nanos.set(25000000); clock.at = BusinessApiTelemetryTest.NOW.minusSeconds(1); if (throwing) throw new IllegalStateException("fixture error"); response.setStatus(422); };
        if (throwing) assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(IllegalStateException.class);
        else filter.doFilter(request, response, chain);
        clock.at = BusinessApiTelemetryTest.NOW.plusSeconds(60); api.flush();
        var point = sink.points.stream().filter(p -> p.name().equals("Duration")).findFirst().orElseThrow();
        assertThat(point.sum()).isEqualTo(25); assertThat(point.status()).isEqualTo(throwing ? "5xx" : "4xx");
      }
    }
  }
  @Test void unauthorizedPublicUnmatchedAndUnsupportedAsyncAreExcluded() throws Exception {
    for (String scenario : List.of("anonymous", "public", "options", "401", "403", "unknown", "denied", "async")) {
      authenticate(); var sink = new BusinessApiTelemetryTest.Sink(); var clock = new BusinessApiTelemetryTest.MutableClock();
      if (scenario.equals("anonymous")) SecurityContextHolder.clearContext();
      try (var api = new BusinessApiTelemetry(true, sink, clock, () -> {}, false)) {
        var request = new MockHttpServletRequest(scenario.equals("options") ? "OPTIONS" : "GET", scenario.equals("public") ? "/actuator/health" : "/admin/user");
        var response = new MockHttpServletResponse();
        if (!scenario.equals("unknown")) request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, new HandlerMethod(this, getClass().getMethod("business")));
        var filter = new ActivityTelemetryFilter(() -> null, () -> api, () -> 0, clock);
        jakarta.servlet.FilterChain chain = (req, res) -> {
          if (scenario.equals("denied")) throw new AccessDeniedException("fixture denied");
          if (scenario.equals("401") || scenario.equals("403")) response.setStatus(Integer.parseInt(scenario));
          if (scenario.equals("async")) { request.setAsyncSupported(true); request.startAsync(); }
        };
        if (scenario.equals("denied")) assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isInstanceOf(AccessDeniedException.class);
        else filter.doFilter(request, response, chain);
        clock.at = BusinessApiTelemetryTest.NOW.plusSeconds(60); api.flush();
        assertThat(sink.points.stream().filter(p -> p.name().equals("Duration"))).as(scenario).isEmpty();
        if (scenario.equals("async")) assertThat(sink.points.stream().filter(p -> p.name().equals("UnsupportedAsync")).mapToDouble(BusinessApiTelemetry.Point::sum).sum()).isEqualTo(1);
      }
    }
  }
}
