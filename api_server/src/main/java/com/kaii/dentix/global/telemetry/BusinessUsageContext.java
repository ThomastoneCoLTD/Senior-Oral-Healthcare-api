package com.kaii.dentix.global.telemetry;

import java.util.function.Supplier;

/** Contains only the current request's live server-verified organization. */
public final class BusinessUsageContext implements AutoCloseable {
  private static final ThreadLocal<BusinessUsageContext> CURRENT=new ThreadLocal<>();
  private final BusinessUsageContext previous;
  private String organization;
  private BusinessUsageContext(){previous=CURRENT.get();CURRENT.set(this);}
  public static BusinessUsageContext open(){return new BusinessUsageContext();}
  public static String organization(){var state=CURRENT.get();return state==null?null:state.organization;}
  public static void capture(Supplier<String> verifiedOrganization) {
    var state=CURRENT.get();if(state==null)return;
    try{state.organization=verifiedOrganization.get();}catch(RuntimeException ignored){state.organization=null;}
  }
  @Override public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
