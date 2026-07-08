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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminDaeguChainTokenService {

    private final ExternalTokenClient externalTokenClient;
    private final DaeguChainProperties properties;
    private static final List<String> REWARD_TOKEN_NAMES = List.of(
            "ESSENTIAL_VIDEO_1",
            "ESSENTIAL_VIDEO_2",
            "ESSENTIAL_VIDEO_3",
            "ESSENTIAL_VIDEO_4",
            "ESSENTIAL_VIDEO_5",
            "OPTIONAL_VIDEO_1",
            "OPTIONAL_VIDEO_2",
            "OPTIONAL_VIDEO_3",
            "OPTIONAL_VIDEO_4",
            "OPTIONAL_VIDEO_5",
            "OPTIONAL_VIDEO_6",
            "OPTIONAL_VIDEO_7"
    );
    private static final Set<String> REWARD_TOKEN_NAME_SET = REWARD_TOKEN_NAMES.stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    private static final Map<String, Integer> REWARD_TOKEN_ORDER = IntStream.range(0, REWARD_TOKEN_NAMES.size())
            .boxed()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    index -> REWARD_TOKEN_NAMES.get(index).toLowerCase(Locale.ROOT),
                    index -> index
            ));

    public List<AdminDaeguChainTokenDto.TokenOption> getTokenOptions() {
        Map<String, AdminDaeguChainTokenDto.TokenOption> tokenOptions = defaultRewardTokenOptions();
        JsonNode tokens;
        try {
            JsonNode data = externalTokenClient.getTokenList();
            tokens = findTokenArray(data);
        } catch (RuntimeException exception) {
            return sortedRewardTokenOptions(tokenOptions);
        }
        if (tokens == null || !tokens.isArray()) {
            return sortedRewardTokenOptions(tokenOptions);
        }

        for (JsonNode token : tokens) {
            String name = extractTokenName(token);
            String contractAddress = extractContractAddress(token);
            if (!isBlank(name) && !isBlank(contractAddress) && isRewardTokenName(name)) {
                tokenOptions.put(
                        name.toLowerCase(Locale.ROOT),
                        AdminDaeguChainTokenDto.TokenOption.builder()
                                .tokenName(name)
                                .contractAddress(contractAddress)
                                .symbol(extractSymbol(token))
                                .supply(extractLong(token, "supply", "total_supply", "totalSupply", "amount"))
                                .decimals(extractInteger(token, "decimals", "decimal"))
                                .owner(extractText(token, "owner", "owner_addr", "ownerAddress"))
                                .issued(extractText(token, "issued", "created", "created_at", "createdAt"))
                                .txHash(extractText(token, "tx_hash", "transaction_hash", "hash"))
                                .factHash(extractText(token, "fact_hash", "factHash"))
                                .build()
                );
            }
        }
        return sortedRewardTokenOptions(tokenOptions);
    }

    private Map<String, AdminDaeguChainTokenDto.TokenOption> defaultRewardTokenOptions() {
        Map<String, AdminDaeguChainTokenDto.TokenOption> tokenOptions = new LinkedHashMap<>();
        for (String tokenName : REWARD_TOKEN_NAMES) {
            tokenOptions.put(
                    tokenName.toLowerCase(Locale.ROOT),
                    AdminDaeguChainTokenDto.TokenOption.builder()
                            .tokenName(tokenName)
                            .contractAddress(tokenName)
                            .build()
            );
        }
        return tokenOptions;
    }

    private List<AdminDaeguChainTokenDto.TokenOption> sortedRewardTokenOptions(
            Map<String, AdminDaeguChainTokenDto.TokenOption> tokenOptions
    ) {
        return tokenOptions.values().stream()
                .sorted(Comparator.comparingInt(option -> rewardTokenOrder(option.getTokenName())))
                .toList();
    }

    public List<String> getTokenNames() {
        return getTokenOptions().stream()
                .map(AdminDaeguChainTokenDto.TokenOption::getTokenName)
                .toList();
    }

    public List<AdminDaeguChainTokenDto.TokenOption> getTokenList() {
        return getTokenOptions();
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
        if (!isRewardTokenName(request.getTokenName())) {
            throw new BadRequestApiException("unsupported reward token name");
        }
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

    private Integer extractInteger(JsonNode token, String... fieldNames) {
        Long value = extractLong(token, fieldNames);
        return value == null ? null : value.intValue();
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

    private boolean isRewardTokenName(String value) {
        return !isBlank(value) && REWARD_TOKEN_NAME_SET.contains(value.toLowerCase(Locale.ROOT));
    }

    private int rewardTokenOrder(String tokenName) {
        return REWARD_TOKEN_ORDER.getOrDefault(tokenName.toLowerCase(Locale.ROOT), Integer.MAX_VALUE);
    }
}
