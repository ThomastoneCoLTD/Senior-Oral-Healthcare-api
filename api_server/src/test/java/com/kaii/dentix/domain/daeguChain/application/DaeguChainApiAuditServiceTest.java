package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.dao.DaeguChainApiLogRepository;
import com.kaii.dentix.domain.daeguChain.domain.DaeguChainApiLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DaeguChainApiAuditServiceTest {

    @Test
    void failureAuditUsesIndependentTransaction() throws Exception {
        Method method = DaeguChainApiAuditService.class.getMethod(
                "recordFailure",
                String.class,
                Object.class,
                RuntimeException.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void recordsMaskedRequestAndResponseForCurrentUser() {
        DaeguChainApiLogRepository repository = mock(DaeguChainApiLogRepository.class);
        DaeguChainApiAuditService service = new DaeguChainApiAuditService(repository, new ObjectMapper());

        DaeguChainApiLogContext.withUser(17L, "구강체조 리워드 지급", () -> {
            service.record(
                    "https://token.example/transfer",
                    Map.of(
                            "token", "secret-app-key",
                            "holder_pkey", "secret-holder-private-key",
                            "receiver", "0x123"
                    ),
                    Map.of(
                            "state", "OK",
                            "data", Map.of("private_key", "secret-private-key", "tx_hash", "0xabc")
                    ),
                    true
            );
            return null;
        });

        ArgumentCaptor<DaeguChainApiLog> captor = ArgumentCaptor.forClass(DaeguChainApiLog.class);
        verify(repository).save(captor.capture());
        DaeguChainApiLog saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(17L);
        assertThat(saved.getFeature()).isEqualTo("구강체조 리워드 지급");
        assertThat(saved.getRequestPayload()).contains("\"token\":\"***\"");
        assertThat(saved.getRequestPayload()).doesNotContain("secret-app-key");
        assertThat(saved.getRequestPayload()).contains("\"holder_pkey\":\"***\"");
        assertThat(saved.getRequestPayload()).doesNotContain("secret-holder-private-key");
        assertThat(saved.getResponsePayload()).contains("\"private_key\":\"***\"");
        assertThat(saved.getResponsePayload()).doesNotContain("secret-private-key");
        assertThat(saved.isSuccess()).isTrue();
    }

    @Test
    void removesSensitiveRequestValuesEchoedInsideFailureMessages() {
        DaeguChainApiLogRepository repository = mock(DaeguChainApiLogRepository.class);
        DaeguChainApiAuditService service = new DaeguChainApiAuditService(repository, new ObjectMapper());
        String rawPrivateKey = "a".repeat(64);

        DaeguChainApiLogContext.withUser(17L, "리워드 지갑 회수 권한 승인", () -> {
            service.recordFailure(
                    "https://chain.example/mitum/token/approve",
                    Map.of("holder_pkey", rawPrivateKey, "holder", "0x123"),
                    new IllegalArgumentException(
                            "holder_pkey 입력값이 개인키 형식이 아닙니다. (" + rawPrivateKey + ")"
                    )
            );
            return null;
        });

        ArgumentCaptor<DaeguChainApiLog> captor = ArgumentCaptor.forClass(DaeguChainApiLog.class);
        verify(repository).save(captor.capture());
        DaeguChainApiLog saved = captor.getValue();

        assertThat(saved.getRequestPayload()).contains("\"holder_pkey\":\"***\"");
        assertThat(saved.getResponsePayload()).contains("holder_pkey 입력값이 개인키 형식이 아닙니다. (***)");
        assertThat(saved.getResponsePayload()).doesNotContain(rawPrivateKey);
    }
}
