package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.config.DadaeguLoginProperties;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DadaeguLoginService {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final DadaeguLoginProperties properties;
    private final UserRepository userRepository;
    private final UserLoginService userLoginService;
    private final ObjectMapper objectMapper;

    public UserDto.DadaeguLoginConfigResponse getConfig() {
        boolean ready = properties.isReady();
        return UserDto.DadaeguLoginConfigResponse.builder()
                .enabled(ready)
                .siteId(ready ? properties.getSiteId() : null)
                .requiredVc(properties.getRequiredVc())
                .build();
    }

    @Transactional
    public UserDto.LoginResponse login(UserDto.DadaeguLoginRequest request) {
        if (!properties.isReady()) {
            throw new BadRequestApiException("다대구 로그인 연동 정보가 설정되지 않았습니다.");
        }

        try {
            JsonNode claimPayload = findClaimPayload(normalizeNode(request.getEncryptedData()), 0);
            if (claimPayload == null) {
                throw new UnauthorizedException("유효한 다대구 인증 결과가 아닙니다.");
            }

            PrivateKey privateKey = parsePrivateKey(properties.getRsaPrivateKey());
            String did = decryptClaim(claimPayload, "did", privateKey);
            String name = decryptClaim(claimPayload, "name", privateKey).trim();
            String birthDate = normalizeBirthDate(decryptClaim(claimPayload, "birthdate", privateKey));
            String phoneNumber = normalizePhoneNumber(decryptClaim(claimPayload, "phoneNumber", privateKey));

            User user = userRepository.findByDaeguDid(did)
                    .or(() -> userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                            phoneNumber,
                            name,
                            birthDate
                    ))
                    .orElseThrow(() -> new UnauthorizedException(
                            "다대구 인증정보와 일치하는 가입 계정이 없습니다. 먼저 사용자 회원가입을 진행해 주세요."
                    ));

            if (user.getIsVerify() != YnType.Y) {
                throw new UnauthorizedException("User is not verified.");
            }

            return userLoginService.completeAuthenticatedLogin(user);
        } catch (UnauthorizedException | BadRequestApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("다대구 인증 결과를 확인할 수 없습니다.");
        }
    }

    private JsonNode normalizeNode(JsonNode node) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String text = node.asText().replace("&quot;", "\"").trim();
            return objectMapper.readTree(text);
        }
        return node;
    }

    private JsonNode findClaimPayload(JsonNode node, int depth) throws Exception {
        if (node == null || node.isNull() || depth > 6) {
            return null;
        }
        JsonNode normalized = normalizeNode(node);
        if (normalized.isObject()
                && normalized.hasNonNull("did")
                && normalized.hasNonNull("name")
                && normalized.hasNonNull("birthdate")
                && normalized.hasNonNull("phoneNumber")) {
            return normalized;
        }
        if (normalized.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = normalized.fields();
            while (fields.hasNext()) {
                JsonNode found = findClaimPayload(fields.next().getValue(), depth + 1);
                if (found != null) {
                    return found;
                }
            }
        } else if (normalized.isArray()) {
            for (JsonNode child : normalized) {
                JsonNode found = findClaimPayload(child, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private PrivateKey parsePrivateKey(String configuredKey) throws Exception {
        String encoded = configuredKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private String decryptClaim(JsonNode payload, String fieldName, PrivateKey privateKey) throws Exception {
        String encryptedValue = payload.path(fieldName).asText("").trim();
        if (encryptedValue.isEmpty()) {
            throw new UnauthorizedException("다대구 필수 인증정보가 누락되었습니다.");
        }
        if ("did".equals(fieldName) && encryptedValue.startsWith("did:daegu:")) {
            return encryptedValue;
        }

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String normalized = phoneNumber.replaceAll("[^0-9]", "");
        if (!normalized.matches("^[0-9]{10,11}$")) {
            throw new UnauthorizedException("다대구 휴대폰 번호 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeBirthDate(String birthDate) {
        String digits = birthDate.replaceAll("[^0-9]", "");
        if (!digits.matches("^\\d{8}$")) {
            throw new UnauthorizedException("다대구 생년월일 형식이 올바르지 않습니다.");
        }
        return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
    }
}
