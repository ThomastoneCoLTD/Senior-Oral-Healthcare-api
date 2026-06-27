package com.kaii.dentix.domain.admin.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDaeguChainTokenService {

    private final ExternalTokenClient externalTokenClient;
    private final DaeguChainProperties properties;

    public List<String> getTokenNames() {
        List<String> tokenNames = new ArrayList<>();
        JsonNode data = externalTokenClient.getTokenList();
        if (data == null || !data.isArray()) {
            return tokenNames;
        }

        for (JsonNode token : data) {
            String name = extractTokenName(token);
            if (!isBlank(name)) {
                tokenNames.add(name);
            }
        }
        return tokenNames;
    }

    public JsonNode getTokenList() {
        return externalTokenClient.getTokenList();
    }

    public DaeguChainDto.ApiResponse<JsonNode> createToken(AdminDaeguChainTokenDto.CreateRequest request) {
        validateTokenCreateConfiguration();
        JsonNode response = externalTokenClient.createToken(
                request.getTokenName(),
                properties.getTokenSymbol(),
                request.getSupply()
        );
        return new DaeguChainDto.ApiResponse<>("OK", null, "", response, null);
    }

    private void validateTokenCreateConfiguration() {
        if (isBlank(properties.getTokenSymbol())) {
            throw new BadRequestApiException("daegu-chain.token-symbol is required");
        }
    }

    private String extractTokenName(JsonNode token) {
        String name = findFirstText(token, "token_name", "tokenName", "name");
        if (!isBlank(name)) {
            return name;
        }
        if (token != null && token.isArray() && token.size() > 1 && token.get(1).isTextual()) {
            return token.get(1).asText();
        }
        return null;
    }

    private String findFirstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                String found = findFirstText(fields.next().getValue(), fieldNames);
                if (!isBlank(found)) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findFirstText(child, fieldNames);
                if (!isBlank(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
