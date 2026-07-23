package com.kaii.dentix.domain.daeguChain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiAuditService;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

@Component
public class ExternalDidClient {

    private final DaeguChainProperties properties;
    private final RestTemplate restTemplate;
    private final DaeguChainApiAuditService auditService;

    public ExternalDidClient(DaeguChainProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties, restTemplateBuilder, null);
    }

    @Autowired
    public ExternalDidClient(
            DaeguChainProperties properties,
            RestTemplateBuilder restTemplateBuilder,
            DaeguChainApiAuditService auditService
    ) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
        this.auditService = auditService;
    }

    public JsonNode createDid() {
        return createDid(Map.of());
    }

    public JsonNode createDid(Map<String, Object> request) {
        JsonNode body = post(properties.getDidCreatePath(), request);
        if (body.has("res") && !body.path("res").asBoolean()) {
            throw new BadRequestApiException("DID server create failed");
        }
        return body;
    }

    private JsonNode post(String path, Map<String, Object> request) {
        Map<String, Object> body = request == null ? Map.of() : request;
        String api = getUrl(path);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    api,
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    JsonNode.class
            );
            JsonNode responseBody = Objects.requireNonNull(response.getBody(), "DID server response body is empty");
            record(api, body, responseBody, true);
            return responseBody;
        } catch (RestClientException | NullPointerException exception) {
            BadRequestApiException apiException =
                    new BadRequestApiException("DID server API call failed: " + exception.getMessage());
            recordFailure(api, body, apiException);
            throw apiException;
        }
    }

    private void record(String api, Object request, Object response, boolean success) {
        if (auditService != null) {
            auditService.record(api, request, response, success);
        }
    }

    private void recordFailure(String api, Object request, RuntimeException exception) {
        if (auditService != null) {
            auditService.recordFailure(api, request, exception);
        }
    }

    private String getUrl(String path) {
        return UriComponentsBuilder
                .fromHttpUrl(trimTrailingSlash(properties.getDidServerBaseUrl()))
                .path(normalizePath(path))
                .toUriString();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
