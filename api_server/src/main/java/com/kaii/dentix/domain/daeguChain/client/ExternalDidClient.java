package com.kaii.dentix.domain.daeguChain.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
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

    public ExternalDidClient(DaeguChainProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    public JsonNode createDid() {
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    getCreateDidUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of()),
                    JsonNode.class
            );
            JsonNode body = Objects.requireNonNull(response.getBody(), "DID server response body is empty");
            if (body.has("res") && !body.path("res").asBoolean()) {
                throw new BadRequestApiException("DID server create failed");
            }
            return body;
        } catch (RestClientException | NullPointerException exception) {
            throw new BadRequestApiException("DID server API call failed: " + exception.getMessage());
        }
    }

    private String getCreateDidUrl() {
        return UriComponentsBuilder
                .fromHttpUrl(trimTrailingSlash(properties.getDidServerBaseUrl()))
                .path(normalizePath(properties.getDidCreatePath()))
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
