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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ExternalTokenClient {

    private final DaeguChainProperties properties;
    private final RestTemplate restTemplate;

    public ExternalTokenClient(DaeguChainProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder.build();
    }

    public JsonNode createToken(String tokenName, String tokenSymbol, Long supply) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token_name", tokenName);
        body.put("token_symbol", tokenSymbol);
        body.put("supply", supply);
        return post(properties.getTokenCreatePath(), body);
    }

    public JsonNode transferToken(String userDid, String tokenName, String receiver, long amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user_DID", userDid);
        body.put("token_name", tokenName);
        body.put("receiver", receiver);
        body.put("wallet_address", receiver);
        body.put("amount", amount);
        return post(properties.getTokenTransferPath(), body);
    }

    public JsonNode getTokenList() {
        return post(properties.getTokenListPath(), Map.of());
    }

    private JsonNode post(String path, Object body) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    getTokenServerUrl(path),
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    JsonNode.class
            );
            JsonNode responseBody = Objects.requireNonNull(response.getBody(), "Token server response body is empty");
            if (responseBody.has("res") && !responseBody.path("res").asBoolean()) {
                String message = responseBody.path("message").asText(responseBody.path("msg").asText("Token server request failed"));
                throw new BadRequestApiException(message);
            }
            return responseBody;
        } catch (RestClientException | NullPointerException exception) {
            throw new BadRequestApiException("Token server API call failed: " + exception.getMessage());
        }
    }

    private String getTokenServerUrl(String path) {
        return UriComponentsBuilder
                .fromHttpUrl(trimTrailingSlash(properties.getTokenServerBaseUrl()))
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
