package com.kaii.dentix.global.common.aop;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.systemLog.dao.SystemLogRepository;
import com.kaii.dentix.domain.errorLog.dao.ErrorLogRepository;
import com.kaii.dentix.domain.errorLog.domain.ErrorLog;
import com.kaii.dentix.domain.systemLog.domain.SystemLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Map;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoggerAspectSecurityTest {
    @AfterEach void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/daegu-chain/token/list", "/admin/daegu-chain/token/reward-transfers",
            "/admin/daegu-chain/token/reward-reclaims", "/daegu-chain/mitum/token/balance"})
    void omittedChainPayloadsRemainValidJsonForDatabaseColumns(String path) throws Exception {
        var mapper = new ObjectMapper();
        var repository = mock(SystemLogRepository.class);
        var logger = new LoggerAspect(mapper, mock(JwtTokenUtil.class), repository, mock(ErrorLogRepository.class));
        var point = chainRequest(path);
        logger.writeSuccessLog(point, Map.of("holder_pkey", "private-response-material", "sensitiveData", "chain-response"));
        var captured = ArgumentCaptor.forClass(SystemLog.class);
        verify(repository).save(captured.capture());
        var saved = captured.getValue();
        assertThat(mapper.readTree(saved.getRequestBody()).asText()).isEqualTo("[chain payload omitted]");
        assertThat(mapper.readTree(saved.getResponseBody()).asText()).isEqualTo("[chain payload omitted]");
        assertThat(saved.getRequestBody() + saved.getResponseBody()).doesNotContain("private-request-material", "private-response-material", "chain-response");
        assertThat(mapper.readTree(saved.getHeader()).path("authorization").asText()).isEqualTo("********");
    }

    @Test void failedChainRequestAlsoStoresValidJsonWithoutRequestSecrets() throws Exception {
        var mapper = new ObjectMapper();
        var repository = mock(ErrorLogRepository.class);
        var logger = new LoggerAspect(mapper, mock(JwtTokenUtil.class), mock(SystemLogRepository.class), repository);
        logger.writeFailLog(chainRequest("/admin/daegu-chain/token/list"), new IllegalStateException("private-error-material"));
        var captured = ArgumentCaptor.forClass(ErrorLog.class);
        verify(repository).save(captured.capture());
        var saved = captured.getValue();
        assertThat(mapper.readTree(saved.getRequestBody()).asText()).isEqualTo("[chain payload omitted]");
        assertThat(saved.getRequestBody()).doesNotContain("private-request-material");
        assertThat(saved.getErrorLogMessage()).isEqualTo("IllegalStateException");
    }

    private JoinPoint chainRequest(String path) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api" + path);
        request.setContextPath("/api");
        request.addHeader("authorization", "private-header-material");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(Fixture.class.getMethod("call", Object.class));
        when(signature.getParameterNames()).thenReturn(new String[]{"payload"});
        when(signature.getParameterTypes()).thenReturn(new Class<?>[]{Object.class});
        var point = mock(JoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{Map.of("holder_pkey", "private-request-material")});
        return point;
    }

    public static class Fixture { @PostMapping public void call(Object payload) {} }

    @Test void nestedSecretsRecoveryAnswersAndPersonalFieldsAreRedacted() {
        var logger = new LoggerAspect(new ObjectMapper(), mock(JwtTokenUtil.class), mock(SystemLogRepository.class), mock(ErrorLogRepository.class));
        var input = Map.of("nested", List.of(Map.of("holder_pkey", "private-material", "mnemonic", "recovery-words", "findPwdAnswer", "personal-answer",
                "userPhoneNumber", "01012345678", "userName", "personal-name", "Cookie", "session-material")), "id", 7);
        String output = ReflectionTestUtils.invokeMethod(logger, "serializeForLog", input);
        assertThat(output).doesNotContain("private-material", "recovery-words", "personal-answer", "01012345678", "personal-name", "session-material")
                .contains("********", "\"id\":7");
    }
}
