package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.client.ExternalDidClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguCredentialStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DaeguChainDidService {

    private final DaeguChainClient daeguChainClient;
    private final ExternalDidClient externalDidClient;
    private final UserRepository userRepository;
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

        String address = findFirstText(externalResponse, "address");
        if (address == null || address.isBlank()) {
            address = extractAddressFromDid(did);
        }
        if (address != null && !address.isBlank()) {
            data.put("address", address);
        }

        JsonNode keyPair = externalResponse.path("data").path("key_pair");
        if (!keyPair.isMissingNode() && !keyPair.isNull()) {
            data.set("key_pair", keyPair);
        }
        String credentialJwt = findFirstText(externalResponse, "credentialJwt", "jwt");
        if (!isBlank(credentialJwt)) {
            data.put("credentialJwt", credentialJwt);
        }
        JsonNode credential = externalResponse.path("data").path("credential");
        if (!credential.isMissingNode() && !credential.isNull()) {
            data.set("credential", credential);
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

    @Transactional
    public DaeguChainDto.ApiResponse<JsonNode> issueLoginUserCredential(Long userId) {
        if (userId == null) {
            throw new BadRequestApiException("userId is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundDataException("User not found"));
        return issueLoginUserCredential(user);
    }

    public DaeguChainDto.ApiResponse<JsonNode> issueLoginUserCredential(User user) {
        if (user == null) {
            throw new BadRequestApiException("user is required");
        }
        if (isBlank(user.getDaeguDid())) {
            throw new BadRequestApiException("user daeguDid is required");
        }
        if (isBlank(user.getUserLoginIdentifier())) {
            throw new BadRequestApiException("userLoginIdentifier is required");
        }

        LocalDate validFrom = resolveCredentialValidFrom();
        LocalDate validUntil = resolveCredentialValidUntil(validFrom);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("issuer", user.getDaeguDid());
        request.put("subject", user.getDaeguDid());
        request.put("claims", Map.of(
                "id", user.getUserLoginIdentifier(),
                "userIdentifier", user.getUserLoginIdentifier()
        ));
        request.put("ttl", resolveCredentialTtlSeconds(validFrom, validUntil));
        request.put("aud", resolveLoginUserCredentialTemplateId());

        JsonNode externalResponse = externalDidClient.issueVc(request);
        String credentialJwt = findFirstText(externalResponse, "vc_jwt", "credentialJwt", "jwt");
        if (isBlank(credentialJwt)) {
            throw new BadRequestApiException("credential jwt is empty");
        }
        user.updateDaeguCredential(
                credentialJwt,
                UserDaeguCredentialStatus.ISSUED,
                validFrom,
                validUntil
        );
        return new DaeguChainDto.ApiResponse<>("OK", Map.of(), "", externalResponse, null);
    }

    public boolean verifyLoginUserCredential(User user) {
        if (user == null
                || isBlank(user.getDaeguDid())
                || isBlank(user.getDaeguCredentialJwt())) {
            return false;
        }

        try {
            JsonNode response = externalDidClient.verifyVc(Map.of(
                    "vc_jwt", user.getDaeguCredentialJwt(),
                    "aud", resolveLoginUserCredentialTemplateId()
            ));
            if (response == null || !response.path("valid").asBoolean(false)) {
                return false;
            }

            JsonNode payload = response.path("payload");
            String issuerDid = findFirstText(payload, "iss");
            String subjectDid = findFirstText(payload, "sub");
            if (!matchesVerifiedDid(user.getDaeguDid(), resolveLoginUserCredentialTemplateId(), issuerDid)
                    || !matchesVerifiedDid(user.getDaeguDid(), resolveLoginUserCredentialTemplateId(), subjectDid)) {
                return false;
            }

            String subject = findCredentialSubject(payload);
            return matchesCredentialSubject(subject, user.getUserLoginIdentifier());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean matchesCredentialSubject(String subject, String userLoginIdentifier) {
        if (isBlank(subject) || isBlank(userLoginIdentifier)) {
            return false;
        }
        return subject.equals(userLoginIdentifier)
                || subject.equals("id|" + userLoginIdentifier)
                || subject.equals("id:" + userLoginIdentifier)
                || subject.equals("id=" + userLoginIdentifier);
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

    private String resolveLoginUserCredentialTemplateId() {
        String templateId = properties.getLoginUserCredentialTemplateId();
        if (isBlank(templateId)) {
            throw new BadRequestApiException("login user credential template id is required");
        }
        return templateId;
    }

    private LocalDate resolveCredentialValidFrom() {
        if (!isBlank(properties.getLoginUserCredentialValidFrom())) {
            return LocalDate.parse(properties.getLoginUserCredentialValidFrom());
        }
        return LocalDate.now();
    }

    private LocalDate resolveCredentialValidUntil(LocalDate validFrom) {
        if (!isBlank(properties.getLoginUserCredentialValidUntil())) {
            return LocalDate.parse(properties.getLoginUserCredentialValidUntil());
        }
        int validDays = properties.getLoginUserCredentialValidDays() == null
                ? 3650
                : Math.max(properties.getLoginUserCredentialValidDays(), 1);
        return validFrom.plusDays(validDays);
    }

    private long resolveCredentialTtlSeconds(LocalDate validFrom, LocalDate validUntil) {
        long validDays = ChronoUnit.DAYS.between(validFrom, validUntil);
        return Math.max(validDays, 1L) * 24L * 60L * 60L;
    }

    private String findCredentialSubject(JsonNode jwtPayload) {
        if (jwtPayload == null || jwtPayload.isNull()) {
            return null;
        }
        for (String claimName : List.of("val", "subject", "credentialSubject", "claims")) {
            JsonNode claim = jwtPayload.get(claimName);
            String subject = extractCredentialSubjectValue(claim);
            if (!isBlank(subject)) {
                return subject;
            }
        }
        JsonNode vcCredentialSubject = jwtPayload.path("vc").path("credentialSubject");
        String subject = extractCredentialSubjectValue(vcCredentialSubject);
        if (!isBlank(subject)) {
            return subject;
        }
        return null;
    }

    private String extractCredentialSubjectValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            if (node.isTextual() && looksLikeJson(node.asText())) {
                try {
                    return extractCredentialSubjectValue(objectMapper.readTree(node.asText()));
                } catch (Exception ignored) {
                    return node.asText();
                }
            }
            return node.asText();
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String subject = extractCredentialSubjectValue(child);
                if (!isBlank(subject)) {
                    return subject;
                }
            }
            return null;
        }
        if (node.isObject()) {
            String directValue = findDirectText(node, "value", "id", "userIdentifier", "userLoginIdentifier");
            if (!isBlank(directValue)) {
                String key = findDirectText(node, "key", "name", "type");
                if (isBlank(key)
                        || "id".equalsIgnoreCase(key)
                        || "userIdentifier".equalsIgnoreCase(key)
                        || "userLoginIdentifier".equalsIgnoreCase(key)) {
                    return directValue;
                }
            }
            return findFirstText(node, "value", "id", "userIdentifier", "userLoginIdentifier");
        }
        return null;
    }

    private String findDirectText(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private boolean looksLikeJson(String value) {
        if (isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean matchesVerifiedDid(String userDid, String templateId, String verifiedDid) {
        if (isBlank(userDid) || isBlank(verifiedDid)) {
            return false;
        }
        String normalizedUserDid = removeTemplateSuffix(userDid, templateId);
        String normalizedVerifiedDid = removeTemplateSuffix(verifiedDid, templateId);
        if (normalizedUserDid.equals(normalizedVerifiedDid)) {
            return true;
        }
        String userAddress = extractAddressFromDid(normalizedUserDid);
        String verifiedAddress = extractAddressFromDid(normalizedVerifiedDid);
        return !isBlank(userAddress) && userAddress.equalsIgnoreCase(verifiedAddress);
    }

    private String removeTemplateSuffix(String did, String templateId) {
        if (isBlank(did) || isBlank(templateId)) {
            return did;
        }
        String suffix = ":" + templateId;
        return did.endsWith(suffix) ? did.substring(0, did.length() - suffix.length()) : did;
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

    private String extractAddressFromDid(String did) {
        if (isBlank(did)) {
            return null;
        }
        int index = did.lastIndexOf(':');
        String candidate = index < 0 || index == did.length() - 1 ? null : did.substring(index + 1);
        return candidate != null && candidate.startsWith("0x") ? candidate : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
