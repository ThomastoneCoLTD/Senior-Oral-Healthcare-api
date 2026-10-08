package com.kaii.dentix.global.telemetry;

import com.kaii.dentix.global.config.WebSecurityConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.time.Clock;
import java.util.function.LongSupplier;
import java.util.Arrays;
import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

public final class ActivityTelemetryFilter extends OncePerRequestFilter {
  private final Supplier<ActivityTelemetry> writer;
  private final Supplier<BusinessApiTelemetry> apiWriter;
  private Supplier<BusinessUsageTelemetry> usageWriter=()->null;
  private final LongSupplier monotonic;
  private final Clock clock;
  private final AntPathMatcher paths=new AntPathMatcher();
  public ActivityTelemetryFilter(Supplier<ActivityTelemetry> writer){this(writer, () -> null);}
  public ActivityTelemetryFilter(Supplier<ActivityTelemetry> writer, Supplier<BusinessApiTelemetry> apiWriter){this(writer, apiWriter, System::nanoTime, Clock.systemUTC());}
  public ActivityTelemetryFilter(Supplier<ActivityTelemetry> writer,Supplier<BusinessApiTelemetry> apiWriter,Supplier<BusinessUsageTelemetry> usageWriter){this(writer,apiWriter);this.usageWriter=usageWriter;}
  ActivityTelemetryFilter(Supplier<ActivityTelemetry> writer, Supplier<BusinessApiTelemetry> apiWriter, LongSupplier monotonic, Clock clock) {
    this.writer=writer; this.apiWriter=apiWriter; this.monotonic=monotonic; this.clock=clock;
  }
  @Override protected boolean shouldNotFilter(HttpServletRequest request) {
    String path=request.getRequestURI().substring(request.getContextPath().length());
    if(path.startsWith("/dentix/"))path=path.substring(7);
    final String route=path;
    if((request.getMethod().equals("GET")&&route.equals("/oral-exercise"))||(request.getMethod().equals("POST")&&route.equals("/admin/account")))return true;
    return request.getMethod().equals("OPTIONS")||path.matches(".*/(health|actuator|login|auto-login|logout|refresh)(/.*)?")||Arrays.stream(WebSecurityConfig.EXCLUDE_URLS).anyMatch(pattern->paths.match(pattern,route));
  }
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    User account=auth!=null&&auth.isAuthenticated()&&auth.getPrincipal() instanceof User a?a:null;
    boolean admin=account!=null&&auth.getAuthorities().stream().anyMatch(a->java.util.Set.of("ROLE_ADMIN","ROLE_SUPER_ADMIN").contains(a.getAuthority()));
    boolean user=account!=null&&auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_USER"));
    boolean denied=false, failed=false;
    long started=monotonic.getAsLong();
    try {chain.doFilter(request,response);}
    catch(ServletException|IOException|RuntimeException error) {
      failed=true;
      for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof AccessDeniedException||cause instanceof AuthenticationException){denied=true;break;}
      throw error;
    } finally {
      if(account!=null&&(admin!=user)&&account.getUsername().matches("[1-9][0-9]*")&&!denied&&response.getStatus()!=401&&response.getStatus()!=403&&request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) instanceof HandlerMethod) {
        double durationMs=Math.max(0,monotonic.getAsLong()-started)/1_000_000.0;
        Instant completedAt=clock.instant();
        try{var usage=usageWriter.get();if(usage!=null&&!request.isAsyncStarted())usage.institution(BusinessUsageContext.organization(),completedAt);}
        catch(RuntimeException ignored){ /* Keep the authorized response unchanged. */ }
        try {
          var telemetry=writer.get();
          if(telemetry!=null)telemetry.record(admin?"admin":"account",account.getUsername(),admin?"admin":"user",Instant.now());
        }catch(RuntimeException ignored){ /* Preserve the existing response, including business errors. */ }
        try {
          var api=apiWriter.get();
          if(api!=null) {
            if(request.isAsyncStarted()) api.unsupportedAsync();
            else api.record(failed?500:response.getStatus(),durationMs,completedAt);
          }
        }catch(RuntimeException ignored){ /* Telemetry never changes authorization or response handling. */ }
      }
    }
  }
}
