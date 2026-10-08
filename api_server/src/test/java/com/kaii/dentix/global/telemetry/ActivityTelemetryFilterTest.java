package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import java.time.*;
import java.util.*;

class ActivityTelemetryFilterTest {
  public void business() {}
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  void authenticate(String role) {
    var authorities=List.of(new SimpleGrantedAuthority(role));
    var account=new User("1","",authorities);
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(account,null,authorities));
  }
  @Test void verifiedAdminAndUserWithSameNumericIdHaveSeparateCohorts() throws Exception {
    var sink=new ActivityTelemetryTest.Sink();
    try(var writer=new ActivityTelemetry(true,sink,Clock.systemUTC(),false)) {
      for(String role:List.of("ROLE_ADMIN","ROLE_USER","ROLE_SUPER_ADMIN")) {
        authenticate(role);
        var request=new MockHttpServletRequest("GET","/user/profile");var response=new MockHttpServletResponse();
        request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,new HandlerMethod(this,getClass().getMethod("business")));
        new ActivityTelemetryFilter(()->writer).doFilter(request,response,(req,res)->{response.setStatus(400);SecurityContextHolder.clearContext();});
        assertThat(response.getStatus()).isEqualTo(400);
      }
      writer.flush();assertThat(sink.events).hasSize(3);
      assertThat(sink.events.get(0).cohort()).isEqualTo("admin");assertThat(sink.events.get(1).cohort()).isEqualTo("account");
      assertThat(sink.events).extracting(ActivityTelemetry.Identity::principal).containsExactly("1","1","1");
      assertThat(sink.events.get(2).cohort()).isEqualTo("admin");
    }
  }
  @Test void deniedUnknownPublicOptionsAndMachineTrafficNeverRecords() throws Exception {
    for(String scenario:List.of("anonymous","machine","options","public","401","403","unknown")) {
      authenticate(scenario.equals("machine")?"ROLE_MACHINE":"ROLE_ADMIN");
      if(scenario.equals("anonymous"))SecurityContextHolder.clearContext();
      var sink=new ActivityTelemetryTest.Sink();
      try(var writer=new ActivityTelemetry(true,sink,Clock.systemUTC(),false)) {
        var request=new MockHttpServletRequest(scenario.equals("options")?"OPTIONS":"GET",scenario.equals("public")?"/actuator/health":"/admin/user");
        if(!scenario.equals("unknown"))request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,new HandlerMethod(this,getClass().getMethod("business")));
        new ActivityTelemetryFilter(()->writer).doFilter(request,new MockHttpServletResponse(),(req,res)->{
          if(scenario.equals("401")||scenario.equals("403"))((jakarta.servlet.http.HttpServletResponse)res).setStatus(Integer.parseInt(scenario));
        });
        writer.flush();assertThat(sink.events).as(scenario).isEmpty();
      }
    }
  }
  @Test void methodAuthorizationFailureIsNotActivity() throws Exception {
    authenticate("ROLE_ADMIN");var sink=new ActivityTelemetryTest.Sink();
    try(var writer=new ActivityTelemetry(true,sink,Clock.systemUTC(),false)) {
      var request=new MockHttpServletRequest("GET","/admin/user");
      request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,new HandlerMethod(this,getClass().getMethod("business")));
      assertThatThrownBy(()->new ActivityTelemetryFilter(()->writer).doFilter(request,new MockHttpServletResponse(),(req,res)->{throw new AccessDeniedException("denied");})).isInstanceOf(AccessDeniedException.class);
      writer.flush();assertThat(sink.events).isEmpty();
    }
  }
}
