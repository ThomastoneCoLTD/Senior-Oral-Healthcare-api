package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.config.DadaeguLoginProperties;
import com.kaii.dentix.domain.user.dao.DadaeguUserIdentityRepository;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.DadaeguSignupSession;
import com.kaii.dentix.domain.user.domain.DadaeguUserIdentity;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DadaeguLoginService {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final DadaeguLoginProperties properties;
    private final UserRepository userRepository;
    private final DadaeguUserIdentityRepository dadaeguUserIdentityRepository;
    private final DadaeguSignupSessionService signupSessionService;
    private final UserLoginService userLoginService;
    private final UserDaeguProvisioningService userDaeguProvisioningService;
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

            DadaeguClaims claims = decryptClaims(claimPayload);
            User user = findExistingUser(claims).orElse(null);

            if (user == null) {
                if (claims.gender() == null) {
                    throw new UnauthorizedException("다대구 인증정보에 성별이 없어 최초 가입을 진행할 수 없습니다.");
                }
                DadaeguSignupSessionService.IssueResult issueResult = signupSessionService.issue(
                        claims.did(),
                        claims.ciHash(),
                        claims.name(),
                        claims.phoneNumber(),
                        claims.birthDate(),
                        claims.gender()
                );
                return UserDto.LoginResponse.builder()
                        .dadaeguOnboardingRequired(true)
                        .dadaeguOnboardingToken(issueResult.token())
                        .dadaeguOnboardingExpiresInSeconds(issueResult.expiresInSeconds())
                        .build();
            }

            if (user.getIsVerify() != YnType.Y) {
                throw new UnauthorizedException("User is not verified.");
            }

            bindIdentity(claims.did(), claims.ciHash(), user);
            userDaeguProvisioningService.provisionForDadaegu(user, claims.did());
            return userLoginService.completeAuthenticatedLogin(user);
        } catch (UnauthorizedException | BadRequestApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("다대구 인증 결과를 확인할 수 없습니다.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UserDto.LoginResponse completeSignUp(UserDto.DadaeguSignUpRequest request) {
        DadaeguSignupSession session = signupSessionService.consume(request.getOnboardingToken());
        DadaeguClaims claims = new DadaeguClaims(
                session.getExternalDid(),
                session.getCiHash(),
                session.getUserName(),
                session.getUserPhoneNumber(),
                session.getUserBirthDate(),
                session.getUserGender()
        );
        if (claims.ciHash() == null || claims.ciHash().isBlank()) {
            throw new BadRequestApiException("다대구 인증정보가 갱신되었습니다. 다대구 로그인을 다시 진행해 주세요.");
        }

        User existingUser = findExistingUser(claims).orElse(null);
        if (existingUser != null) {
            if (existingUser.getIsVerify() != YnType.Y) {
                throw new UnauthorizedException("User is not verified.");
            }
            bindIdentity(claims.did(), claims.ciHash(), existingUser);
            userDaeguProvisioningService.provisionForDadaegu(existingUser, claims.did());
            return userLoginService.completeAuthenticatedLogin(existingUser);
        }

        User user = userLoginService.createDadaeguUser(
                generateLoginIdentifier(),
                claims.name(),
                claims.gender(),
                claims.phoneNumber(),
                claims.birthDate(),
                request.getRealOrganization(),
                request.getUserServiceAgreementRequest()
        );
        bindIdentity(claims.did(), claims.ciHash(), user);
        userDaeguProvisioningService.provisionForDadaegu(user, claims.did());
        return userLoginService.completeAuthenticatedLogin(user);
    }

    private DadaeguClaims decryptClaims(JsonNode claimPayload) throws Exception {
        PrivateKey privateKey = parsePrivateKey(properties.getRsaPrivateKey());
        String did = decryptClaim(claimPayload, "did", privateKey);
        String ciHash = hashCi(decryptClaim(claimPayload, "ci", privateKey));
        String name = decryptClaim(claimPayload, "name", privateKey).trim();
        String birthDate = normalizeBirthDate(decryptClaim(claimPayload, "birthdate", privateKey));
        String phoneNumber = normalizePhoneNumber(decryptClaim(claimPayload, "phoneNumber", privateKey));
        String genderValue = decryptOptionalClaim(claimPayload, "gender", privateKey);
        GenderType gender = genderValue == null ? null : normalizeGender(genderValue);
        return new DadaeguClaims(did, ciHash, name, phoneNumber, birthDate, gender);
    }

    private Optional<User> findExistingUser(DadaeguClaims claims) {
        Optional<DadaeguUserIdentity> didIdentity =
                dadaeguUserIdentityRepository.findByExternalDid(claims.did());
        Optional<DadaeguUserIdentity> ciIdentity =
                dadaeguUserIdentityRepository.findByCiHash(claims.ciHash());

        if (didIdentity.isPresent() && ciIdentity.isPresent()
                && !didIdentity.get().getUserId().equals(ciIdentity.get().getUserId())) {
            throw new UnauthorizedException("다대구 DID와 CI가 서로 다른 SOH 계정에 연결되어 있습니다.");
        }

        Optional<DadaeguUserIdentity> mappedIdentity = didIdentity.isPresent() ? didIdentity : ciIdentity;
        if (mappedIdentity.isPresent()) {
            User mappedUser = userRepository.findById(mappedIdentity.get().getUserId())
                    .orElseThrow(() -> new UnauthorizedException("연결된 SOH 계정을 찾을 수 없습니다."));
            return Optional.of(mappedUser);
        }
        return userRepository.findByUserPhoneNumberAndUserNameAndUserBirthDate(
                claims.phoneNumber(),
                claims.name(),
                claims.birthDate()
        );
    }

    private void bindIdentity(String externalDid, String ciHash, User user) {
        Optional<DadaeguUserIdentity> externalIdentity =
                dadaeguUserIdentityRepository.findByExternalDid(externalDid);
        if (externalIdentity.isPresent()) {
            DadaeguUserIdentity identity = externalIdentity.get();
            if (!identity.getUserId().equals(user.getUserId())) {
                throw new UnauthorizedException("이미 다른 SOH 계정에 연결된 다대구 인증정보입니다.");
            }
            assertCiMatches(identity, ciHash);
            identity.updateCiHash(ciHash);
            dadaeguUserIdentityRepository.saveAndFlush(identity);
            return;
        }

        Optional<DadaeguUserIdentity> ciIdentity = dadaeguUserIdentityRepository.findByCiHash(ciHash);
        if (ciIdentity.isPresent()) {
            DadaeguUserIdentity identity = ciIdentity.get();
            if (!identity.getUserId().equals(user.getUserId())) {
                throw new UnauthorizedException("이미 다른 다대구 인증정보가 SOH 계정에 연결되어 있습니다.");
            }
            identity.updateExternalDid(externalDid);
            dadaeguUserIdentityRepository.saveAndFlush(identity);
            return;
        }

        Optional<DadaeguUserIdentity> userIdentity =
                dadaeguUserIdentityRepository.findByUserId(user.getUserId());
        if (userIdentity.isPresent()) {
            DadaeguUserIdentity identity = userIdentity.get();
            if (!identity.getExternalDid().equals(externalDid)) {
                if (identity.getCiHash() != null && !identity.getCiHash().isBlank()) {
                    throw new UnauthorizedException("이미 다른 다대구 계정이 연결되어 있습니다.");
                }
                identity.updateExternalDid(externalDid);
            }
            assertCiMatches(identity, ciHash);
            identity.updateCiHash(ciHash);
            dadaeguUserIdentityRepository.saveAndFlush(identity);
            return;
        }

        dadaeguUserIdentityRepository.saveAndFlush(DadaeguUserIdentity.builder()
                .userId(user.getUserId())
                .externalDid(externalDid)
                .ciHash(ciHash)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void assertCiMatches(DadaeguUserIdentity identity, String ciHash) {
        if (identity.getCiHash() != null
                && !identity.getCiHash().isBlank()
                && !identity.getCiHash().equals(ciHash)) {
            throw new UnauthorizedException("다대구 CI가 기존 연결정보와 일치하지 않습니다.");
        }
    }

    private String generateLoginIdentifier() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "dg" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            if (userRepository.findByUserLoginIdentifier(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new BadRequestApiException("다대구 사용자 계정을 생성할 수 없습니다. 다시 시도해 주세요.");
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
                && normalized.hasNonNull("ci")
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

    private String decryptOptionalClaim(JsonNode payload, String fieldName, PrivateKey privateKey) throws Exception {
        String encryptedValue = payload.path(fieldName).asText("").trim();
        if (encryptedValue.isEmpty()) {
            return null;
        }
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String hashCi(String ci) {
        if (ci == null || ci.isBlank()) {
            throw new UnauthorizedException("다대구 CI가 누락되었습니다.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(ci.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("DaDaegu CI hashing is unavailable.", exception);
        }
    }

    private GenderType normalizeGender(String gender) {
        String normalized = gender == null
                ? ""
                : gender.trim()
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9가-힣]", "");

        if (normalized.equals("W")
                || normalized.equals("F")
                || normalized.equals("FEMALE")
                || normalized.equals("WOMAN")
                || normalized.equals("여")
                || normalized.equals("여성")
                || normalized.equals("여자")
                || normalized.matches("[2468]")) {
            return GenderType.W;
        }
        if (normalized.equals("M")
                || normalized.equals("MALE")
                || normalized.equals("MAN")
                || normalized.equals("남")
                || normalized.equals("남성")
                || normalized.equals("남자")
                || normalized.matches("[1357]")) {
            return GenderType.M;
        }
        if (normalized.contains("FEMALE")
                || normalized.contains("WOMAN")
                || normalized.contains("여")) {
            return GenderType.W;
        }
        if (normalized.contains("MALE")
                || normalized.contains("MAN")
                || normalized.contains("남")) {
            return GenderType.M;
        }
        throw new UnauthorizedException("다대구 성별 정보를 확인할 수 없습니다.");
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

    private record DadaeguClaims(
            String did,
            String ciHash,
            String name,
            String phoneNumber,
            String birthDate,
            GenderType gender
    ) {
    }
}
