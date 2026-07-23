package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.dao.DaeguChainApiLogRepository;
import com.kaii.dentix.domain.daeguChain.domain.DaeguChainApiLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DaeguChainApiAuditServiceTest {

    @Test
    void recordsMaskedRequestAndResponseForCurrentUser() {
        DaeguChainApiLogRepository repository = mock(DaeguChainApiLogRepository.class);
        DaeguChainApiAuditService service = new DaeguChainApiAuditService(repository, new ObjectMapper());

        DaeguChainApiLogContext.withUser(17L, "구강체조 리워드 지급", () -> {
            service.record(
                    "https://token.example/transfer",
                    Map.of("token", "secret-app-key", "receiver", "0x123"),
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
        assertThat(saved.getResponsePayload()).contains("\"private_key\":\"***\"");
        assertThat(saved.getResponsePayload()).doesNotContain("secret-private-key");
        assertThat(saved.isSuccess()).isTrue();
    }
}
