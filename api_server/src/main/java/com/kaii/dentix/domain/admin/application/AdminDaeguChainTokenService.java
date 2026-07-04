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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDaeguChainTokenService {

    private final ExternalTokenClient externalTokenClient;
    private final DaeguChainProperties properties;

    public List<AdminDaeguChainTokenDto.TokenOption> getTokenOptions() {
        Map<String, AdminDaeguChainTokenDto.TokenOption> tokenOptions = new LinkedHashMap<>();
        JsonNode data = externalTokenClient.getTokenList();
        JsonNode tokens = findTokenArray(data);
        if (tokens == null || !tokens.isArray()) {
            return List.of();
        }

        for (JsonNode token : tokens) {
            String name = extractTokenName(token);
            String contractAddress = extractContractAddress(token);
            if (!isBlank(name) && !isBlank(contractAddress)) {
                tokenOptions.putIfAbsent(
                        name.toLowerCase(Locale.ROOT),
                        AdminDaeguChainTokenDto.TokenOption.builder()
                                .tokenName(name)
                                .contractAddress(contractAddress)
                                .symbol(extractSymbol(token))
                                .supply(extractLong(token, "supply", "total_supply", "totalSupply", "amount"))
                                .issued(extractText(token, "issued", "created", "created_at", "createdAt"))
                                .build()
                );
            }
        }
        return new ArrayList<>(tokenOptions.values());
    }

    public List<String> getTokenNames() {
        return getTokenOptions().stream()
                .map(AdminDaeguChainTokenDto.TokenOption::getTokenName)
                .toList();
    }

    public JsonNode getTokenList() {
        return externalTokenClient.getTokenList();
    }

    private JsonNode findTokenArray(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        if (payload.isArray()) {
            return payload;
        }
        for (String fieldName : List.of("response", "data", "result", "tokens", "tokenList", "token_list")) {
            JsonNode child = payload.get(fieldName);
            JsonNode found = findTokenArray(child);
            if (found != null && found.isArray()) {
                return found;
            }
        }
        return null;
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

    private String extractContractAddress(JsonNode token) {
        return findFirstText(token, "contract", "contract_address", "contractAddress", "cont_addr", "address");
    }

    private String extractSymbol(JsonNode token) {
        return findFirstText(token, "token_symbol", "tokenSymbol", "symbol");
    }

    private String extractText(JsonNode token, String... fieldNames) {
        return findFirstText(token, fieldNames);
    }

    private Long extractLong(JsonNode token, String... fieldNames) {
        JsonNode value = findFirstNode(token, fieldNames);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String findFirstText(JsonNode node, String... fieldNames) {
        JsonNode value = findFirstNode(node, fieldNames);
        return value == null || value.isNull() ? null : value.asText();
    }

    private JsonNode findFirstNode(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value;
            }
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode found = findFirstNode(fields.next().getValue(), fieldNames);
                if (found != null && !found.isNull() && !found.asText().isBlank()) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findFirstNode(child, fieldNames);
                if (found != null && !found.isNull() && !found.asText().isBlank()) {
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
