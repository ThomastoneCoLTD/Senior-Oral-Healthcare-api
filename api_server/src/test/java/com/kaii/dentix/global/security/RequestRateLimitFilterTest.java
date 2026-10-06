package com.kaii.dentix.global.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestRateLimitFilterTest {
    @Test void spoofedForwardedHeaderDoesNotBypassLimitAndWindowResets() throws Exception {
        Clock clock = mock(Clock.class); when(clock.millis()).thenReturn(60_000L);
        var filter = new RequestRateLimitFilter(clock);
        var completed = new AtomicInteger();
        for (int i = 0; i < 240; i++) {
            var response = new MockHttpServletResponse();
            filter.doFilter(request("/login", Integer.toString(i)), response, (req, res) -> completed.incrementAndGet());
            assertThat(response.getStatus()).isEqualTo(200);
        }
        var throttled = new MockHttpServletResponse();
        filter.doFilter(request("/login", "new-fake-ip"), throttled, (req, res) -> completed.incrementAndGet());
        assertThat(throttled.getStatus()).isEqualTo(429);
        assertThat(throttled.getHeader("Retry-After")).isEqualTo("60");
        assertThat(completed.get()).isEqualTo(240);
        when(clock.millis()).thenReturn(120_000L);
        var nextWindow = new MockHttpServletResponse();
        filter.doFilter(request("/login", "new-fake-ip"), nextWindow, (req, res) -> completed.incrementAndGet());
        assertThat(nextWindow.getStatus()).isEqualTo(200);
        assertThat(completed.get()).isEqualTo(241);
    }
    @Test void excludedDadaeguFlowIsUnchanged() throws Exception {
        var filter = new RequestRateLimitFilter(); var completed = new AtomicInteger();
        for (int i = 0; i < 250; i++) filter.doFilter(request("/login/dadaegu", ""), new MockHttpServletResponse(), (req,res) -> completed.incrementAndGet());
        assertThat(completed.get()).isEqualTo(250);
    }
    private MockHttpServletRequest request(String path, String forwarded) {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("192.0.2.10"); request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }
}
