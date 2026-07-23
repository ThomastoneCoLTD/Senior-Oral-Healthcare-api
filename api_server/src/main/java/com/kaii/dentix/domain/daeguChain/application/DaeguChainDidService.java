package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.client.ExternalDidClient;
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
public class DaeguChainDidService {

    private final DaeguChainClient daeguChainClient;
    private final ExternalDidClient externalDidClient;
    private final DaeguChainProperties properties;
    private final ObjectMapper objectMapper;

    public DaeguChainDto.ApiResponse<JsonNode> projectList(Map<String, Object> request) {
        return call("/mitum/did/projects", request, List.of());
    }

    public DaeguChainDto.ApiResponse<JsonNode> registProject(Map<String, Object> request) {
        return call("/mitum/did/regist_project", request, List.of(
                "operation",
                "project_id",
                "project_name",
                "issuer_name",
                "company_name",
                "service_name",
                "display_name",
                "service_url",
                "icon_url"
        ));
    }

    public DaeguChainDto.ApiResponse<JsonNode> templateList(Map<String, Object> request) {
        return call("/mitum/did/templates", request, List.of("project_id"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> editTemplate(Map<String, Object> request) {
        return call("/mitum/did/edit_template", request, List.of("project_id", "operation"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> accountList(Map<String, Object> request) {
        return call("/mitum/did/accounts", request, List.of());
    }

    public DaeguChainDto.ApiResponse<JsonNode> createAccount(Map<String, Object> request) {
        Map<String, Object> body = request == null ? Map.of() : new LinkedHashMap<>(request);
        JsonNode externalResponse = externalDidClient.createDid(body);
        String did = findFirstText(externalResponse, "DID", "did");
        if (did == null || did.isBlank()) {
            throw new BadRequestApiException("DID server response did is empty");
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("did", did);
        data.put("DID", did);

        String address = findFirstText(
                externalResponse,
                "walletAddress",
                "wallet_address",
                "accountAddress",
                "account_address",
                "address"
        );
        if (address != null && !address.isBlank()) {
            data.put("address", address);
            data.put("walletAddress", address);
        }

        JsonNode keyPair = externalResponse.path("data").path("key_pair");
        if (!keyPair.isMissingNode() && !keyPair.isNull()) {
            data.set("key_pair", keyPair);
        }
        data.set("external_response", externalResponse);

        return new DaeguChainDto.ApiResponse<>("OK", Map.of(), "", data, null);
    }

    public DaeguChainDto.ApiResponse<JsonNode> getKey(Map<String, Object> request) {
        return call("/mitum/did/get_key", request, List.of("did"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> issueCredential(Map<String, Object> request) {
        return call("/mitum/did/issue", request, List.of("did", "template_id", "subject", "validfrom", "validuntil"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> disclosure(Map<String, Object> request) {
        return call("/mitum/did/disclosure", request, List.of("did", "template_id"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> verification(Map<String, Object> request) {
        return call("/mitum/did/verification", request, List.of("template_id", "jwt"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> revokeCredential(Map<String, Object> request) {
        return call("/mitum/did/revoke", request, List.of("jwt"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> qrCode(Map<String, Object> request) {
        return call("/mitum/did/qrcode", request, List.of("did", "template_id"));
    }

    private DaeguChainDto.ApiResponse<JsonNode> call(String path, Map<String, Object> request, List<String> requiredFields) {
        Map<String, Object> body = new LinkedHashMap<>(request == null ? Map.of() : request);
        body.putIfAbsent("token", resolveToken((String) body.get("token")));
        body.putIfAbsent("chain", resolveChain((String) body.get("chain")));
        validateRequiredFields(body, requiredFields);
        return daeguChainClient.postDid(path, body);
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

    private String findFirstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                if (value.isArray()) {
                    for (JsonNode child : value) {
                        if (child != null && !child.isNull() && !child.asText().isBlank()) {
                            return child.asText();
                        }
                    }
                } else if (!value.asText().isBlank()) {
                    return value.asText();
                }
            }
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                String found = findFirstText(fields.next().getValue(), fieldNames);
                if (found != null && !found.isBlank()) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findFirstText(child, fieldNames);
                if (found != null && !found.isBlank()) {
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
