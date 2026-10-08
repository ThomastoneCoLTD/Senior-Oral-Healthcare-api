package com.kaii.dentix.global.telemetry;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.kaii.dentix.domain.jwt.*;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.user.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

class ActivityAuthenticationBoundaryTest {
  public void business() {}
  @Test void existingJwtValidationRejectsForgedExpiredAndDeletedAccountsBeforeActivity() throws Exception {
    String key="fixture-only-signing-key-32bytes!!";
    var users=mock(UserRepository.class);var admins=mock(AdminRepository.class);
    var tokens=new JwtTokenUtil(users,admins);
    ReflectionTestUtils.setField(tokens,"accessTokenKeyRaw",key);
    ReflectionTestUtils.setField(tokens,"refreshTokenKeyRaw",key);
    ReflectionTestUtils.invokeMethod(tokens,"init");
    for(String scenario:List.of("valid","forged","expired","deleted","revoked")) {
      SecurityContextHolder.clearContext();
      var user=mock(User.class);when(user.getUserId()).thenReturn(1L);
      String refresh=tokens.createToken(user,TokenType.RefreshToken);
      when(user.getUserRefreshToken()).thenReturn(refresh);
      when(users.findById(1L)).thenReturn(scenario.equals("deleted")?Optional.empty():Optional.of(user));
      var signing=Keys.hmacShaKeyFor((scenario.equals("forged")?"another-fixture-signing-key-32!!!":key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      String token=Jwts.builder().setSubject("1").claim("roles","ROLE_USER").setExpiration(Date.from(Instant.now().plusSeconds(scenario.equals("expired")?-60:60))).signWith(signing).compact();
      if(scenario.equals("valid")||scenario.equals("revoked"))token=tokens.createToken(user,TokenType.AccessToken);
      if(scenario.equals("revoked"))when(user.getUserRefreshToken()).thenReturn(null);
      var request=new MockHttpServletRequest("GET","/user/profile");request.addHeader("Authorization","Bearer "+token);
      request.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE,new HandlerMethod(this,getClass().getMethod("business")));
      var sink=new ActivityTelemetryTest.Sink();var response=new MockHttpServletResponse();
      try(var writer=new ActivityTelemetry(true,sink,Clock.systemUTC(),false)) {
        new JwtAuthenticationFilter(tokens).doFilter(request,response,(req,res)->new ActivityTelemetryFilter(()->writer).doFilter(req,res,(r,s)->response.setStatus(SecurityContextHolder.getContext().getAuthentication()==null?401:500)));
        writer.flush();assertThat(sink.events).as(scenario).hasSize(scenario.equals("valid")?1:0);
        assertThat(response.getStatus()).isEqualTo(scenario.equals("valid")?500:401);
      }finally{SecurityContextHolder.clearContext();}
    }
  }
}
