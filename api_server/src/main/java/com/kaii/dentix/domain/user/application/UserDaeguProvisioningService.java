package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguCredentialStatus;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDaeguProvisioningService {

    private final DaeguChainDidService daeguChainDidService;
    private final UserRewardWalletRepository userRewardWalletRepository;

    public void provisionForSignUp(User user) {
        String walletAddress = provisionDid(user);
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED) {
            return;
        }
        provisionWallet(user, walletAddress);
        if (isBlank(user.getDaeguCredentialJwt())) {
            provisionCredential(user);
        }
    }

    private String provisionDid(User user) {
        try {
            DaeguChainDto.ApiResponse<JsonNode> response = daeguChainDidService.createAccount(buildDidCreateRequest(user));
            JsonNode data = response.getData();
            String did = findFirstText(data, "did", "DID", "account");
            String key = findFirstText(data, "publickey", "public_key", "publicKey", "key_id", "keyId");
            String walletAddress = findFirstText(data, "address");
            String credentialJwt = findFirstText(data, "credentialJwt", "jwt");
            if (isBlank(walletAddress)) {
                walletAddress = extractAddressFromDid(did);
            }
            if (isBlank(did)) {
                throw new BadRequestApiException("DaeguChain DID is empty");
            }
            user.updateDaeguDid(did, key, UserDaeguIdentityStatus.ISSUED);
            if (!isBlank(credentialJwt)) {
                user.updateDaeguCredential(
                        credentialJwt,
                        UserDaeguCredentialStatus.ISSUED,
                        null,
                        null
                );
            }
            return walletAddress;
        } catch (RuntimeException exception) {
            log.warn("Daegu DID provisioning failed. userId={}", user.getUserId(), exception);
            user.updateDaeguDid(null, null, UserDaeguIdentityStatus.FAILED);
            throw new BadRequestApiException("DID 생성에 실패했습니다: " + exception.getMessage());
        }
    }

    private Map<String, Object> buildDidCreateRequest(User user) {
        if (user == null || isBlank(user.getUserLoginIdentifier())) {
            return Map.of();
        }
        return Map.of(
                "userIdentifier", user.getUserLoginIdentifier(),
                "label", user.getUserLoginIdentifier()
        );
    }

    private void provisionWallet(User user, String didWalletAddress) {
        userRewardWalletRepository.findByUserId(user.getUserId()).ifPresentOrElse(
                wallet -> {
                    if (isBlank(wallet.getWalletAddress())) {
                        wallet.updateDaeguWallet(user.getDaeguDid(), resolveWalletAddress(user, didWalletAddress));
                        userRewardWalletRepository.save(wallet);
                    }
                },
                () -> userRewardWalletRepository.save(UserRewardWallet.builder()
                        .userId(user.getUserId())
                        .pointBalance(0L)
                        .daeguDid(user.getDaeguDid())
                        .walletAddress(resolveWalletAddress(user, didWalletAddress))
                        .build())
        );
    }

    private void provisionCredential(User user) {
        try {
            daeguChainDidService.issueLoginUserCredential(user);
            if (isBlank(user.getDaeguCredentialJwt())
                    || user.getDaeguCredentialStatus() != UserDaeguCredentialStatus.ISSUED) {
                throw new BadRequestApiException("credential jwt is empty");
            }
        } catch (RuntimeException exception) {
            log.warn("Daegu DID credential issuance failed. userId={}", user.getUserId(), exception);
            user.markDaeguCredentialFailed();
        }
    }

    private String resolveWalletAddress(User user, String didWalletAddress) {
        if (!isBlank(didWalletAddress)) {
            return didWalletAddress;
        }
        return extractAddressFromDid(user.getDaeguDid());
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

    private String extractAddressFromDid(String did) {
        if (isBlank(did)) {
            return null;
        }
        int index = did.lastIndexOf(':');
        return index < 0 || index == did.length() - 1 ? null : did.substring(index + 1);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
