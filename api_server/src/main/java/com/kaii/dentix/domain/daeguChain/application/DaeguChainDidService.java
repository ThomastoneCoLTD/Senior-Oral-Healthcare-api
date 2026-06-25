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
public class DaeguChainDidService {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

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
        return call("/mitum/did/create_account", request, List.of());
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
}
