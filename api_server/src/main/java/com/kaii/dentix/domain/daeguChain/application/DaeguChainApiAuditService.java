package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.daeguChain.dao.DaeguChainApiLogRepository;
import com.kaii.dentix.domain.daeguChain.domain.DaeguChainApiLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DaeguChainApiAuditService {

    private static final int MAX_PAYLOAD_LENGTH = 16_000;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "token",
            "appkey",
            "privatekey",
            "pkey",
            "ownerpkey",
            "senderpkey",
            "holderpkey",
            "jwt",
            "password",
            "secret",
            "authorization"
    );

    private final DaeguChainApiLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String api, Object request, Object response, boolean success) {
        DaeguChainApiLogContext.Actor actor = DaeguChainApiLogContext.current();
        if (actor == null || actor.userId() == null) {
            return;
        }
        try {
            repository.save(DaeguChainApiLog.builder()
                    .userId(actor.userId())
                    .feature(actor.feature())
                    .api(api == null ? "" : api)
                    .requestPayload(toSafeJson(request))
                    .responsePayload(toSafeJson(response))
                    .success(success)
                    .build());
        } catch (RuntimeException exception) {
            log.warn("Unable to save DaeguChain API audit log. api={}", api, exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String api, Object request, RuntimeException exception) {
        record(
                api,
                request,
                Map.of(
                        "errorType", exception.getClass().getSimpleName(),
                        "message", sanitizeErrorMessage(request, exception.getMessage())
                ),
                false
        );
    }

    public String sanitizeErrorMessage(Object request, String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        try {
            Set<String> sensitiveValues = new LinkedHashSet<>();
            collectSensitiveValues(objectMapper.valueToTree(request == null ? Map.of() : request), sensitiveValues);
            String sanitized = message;
            List<String> valuesByLength = sensitiveValues.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                    .toList();
            for (String sensitiveValue : valuesByLength) {
                sanitized = sanitized.replace(sensitiveValue, "***");
            }
            return sanitized;
        } catch (RuntimeException exception) {
            return "DaeguChain upstream request failed";
        }
    }

    private String toSafeJson(Object value) {
        try {
            JsonNode node = objectMapper.valueToTree(value == null ? Map.of() : value);
            sanitize(node);
            return truncate(objectMapper.writeValueAsString(node));
        } catch (Exception exception) {
            return "{\"serializationError\":\"payload could not be serialized\"}";
        }
    }

    private void sanitize(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    objectNode.put(field.getKey(), "***");
                } else {
                    sanitize(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::sanitize);
        }
    }

    private void collectSensitiveValues(JsonNode node, Set<String> values) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    JsonNode value = field.getValue();
                    if (value != null && value.isValueNode()) {
                        values.add(value.asText());
                    }
                } else {
                    collectSensitiveValues(field.getValue(), values);
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(child -> collectSensitiveValues(child, values));
        }
    }

    private boolean isSensitive(String key) {
        String normalized = key == null
                ? ""
                : key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return SENSITIVE_KEYS.contains(normalized);
    }

    private String truncate(String value) {
        if (value.length() <= MAX_PAYLOAD_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
    }
}
