package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DaeguChainTimestampService {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<JsonNode> projectList(Map<String, Object> request) {
        return call("/mitum/ts/projects", request, List.of());
    }

    public DaeguChainDto.ApiResponse<JsonNode> registProject(Map<String, Object> request) {
        return call("/mitum/ts/regist_project", request, List.of("operation"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> request(Map<String, Object> request) {
        return call("/mitum/ts/request", request, List.of("project_id", "request_ts", "timestamp_key", "timestamp_data"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getKeyByHash(Map<String, Object> request) {
        return call("/mitum/ts/get_key", request, List.of("project_id", "tx_hash"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTimestamp(Map<String, Object> request) {
        return call("/mitum/ts/get_ts", request, List.of("project_id", "timestamp_key"));
    }

    private DaeguChainDto.ApiResponse<JsonNode> call(String path, Map<String, Object> request, List<String> requiredFields) {
        Map<String, Object> body = new LinkedHashMap<>(request == null ? Map.of() : request);
        body.putIfAbsent("token", resolveToken((String) body.get("token")));
        body.putIfAbsent("chain", resolveChain((String) body.get("chain")));
        validateRequiredFields(body, requiredFields);
        return daeguChainClient.postTimestamp(path, body);
    }

    private void validateRequiredFields(Map<String, Object> body, List<String> requiredFields) {
        for (String field : requiredFields) {
            Object value = body.get(field);
            if (value == null || (value instanceof String stringValue && stringValue.isBlank())) {
                throw new BadRequestApiException(field + " is required");
            }
        }
    }

    private String resolveChain(String chain) {
        return chain == null || chain.isBlank() ? properties.getChain() : chain;
    }

    private String resolveToken(String token) {
        String configuredAppKey = properties.resolveAppKey();
        String resolvedToken = configuredAppKey == null || configuredAppKey.isBlank() ? token : configuredAppKey;
        if (resolvedToken == null || resolvedToken.isBlank()) {
            throw new BadRequestApiException("token is required");
        }
        return resolvedToken;
    }
}
