package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DaeguChainTimestampServiceTest {

    private DaeguChainClient daeguChainClient;
    private DaeguChainTimestampService service;

    @BeforeEach
    void setUp() {
        daeguChainClient = mock(DaeguChainClient.class);
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setChain("dchain");
        properties.setToken("configured-token");
        service = new DaeguChainTimestampService(daeguChainClient, properties);
    }

    @Test
    void projectListUsesConfiguredTokenAndChainWhenRequestOmitsThem() {
        service.projectList(null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postTimestamp(eq("/mitum/ts/projects"), captor.capture());
        assertThat(captor.getValue().get("token")).isEqualTo("configured-token");
        assertThat(captor.getValue().get("chain")).isEqualTo("dchain");
    }

    @Test
    void requestRequiresTimestampKey() {
        assertThatThrownBy(() -> service.request(Map.of(
                "project_id", "PROJECTID",
                "request_ts", 1724002357398L,
                "timestamp_data", "1696488110000"
        )))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("timestamp_key is required");
    }

    @Test
    void getTimestampPostsExpectedPath() {
        service.getTimestamp(Map.of(
                "project_id", "PROJECTID",
                "timestamp_key", "testTSkey"
        ));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainClient).postTimestamp(eq("/mitum/ts/get_ts"), captor.capture());
        assertThat(captor.getValue().get("project_id")).isEqualTo("PROJECTID");
        assertThat(captor.getValue().get("timestamp_key")).isEqualTo("testTSkey");
    }
}
