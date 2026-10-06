package com.kaii.dentix.domain.jwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void downstreamExceptionIsNotCaughtOrExecutedTwice() throws Exception {
        var jwt = mock(JwtTokenUtil.class); var filter = new JwtAuthenticationFilter(jwt);
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class); doThrow(new ServletException("downstream failure")).when(chain).doFilter(request, response);
        assertThatThrownBy(() -> filter.doFilter(request,response,chain)).isInstanceOf(ServletException.class);
        verify(chain, times(1)).doFilter(request,response);
    }
}
