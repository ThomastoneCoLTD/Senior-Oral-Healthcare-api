package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDaeguProvisioningService {

    private final DaeguChainDidService daeguChainDidService;
    private final DaeguChainAccountService daeguChainAccountService;
    private final UserRewardWalletRepository userRewardWalletRepository;
    private final Environment environment;

    public void provisionForSignUp(User user) {
        provisionDid(user);
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED) {
            return;
        }
        provisionWallet(user);
    }

    private void provisionDid(User user) {
        try {
            DaeguChainDto.ApiResponse<JsonNode> response = daeguChainDidService.createAccount(Map.of());
            JsonNode data = response.getData();
            String did = findFirstText(data, "did", "DID", "account");
            String key = findFirstText(data, "public_key", "publicKey", "key_id", "keyId");
            if (isBlank(did)) {
                throw new BadRequestApiException("DaeguChain DID is empty");
            }
            user.updateDaeguDid(did, key, UserDaeguIdentityStatus.ISSUED);
        } catch (RuntimeException exception) {
            log.warn("Daegu DID provisioning failed. userId={}", user.getUserId(), exception);
            user.updateDaeguDid(null, null, UserDaeguIdentityStatus.FAILED);
        }
    }

    private void provisionWallet(User user) {
        userRewardWalletRepository.findByUserId(user.getUserId()).ifPresentOrElse(
                wallet -> {
                    if (isBlank(wallet.getWalletAddress())) {
                        wallet.updateDaeguWallet(user.getDaeguDid(), createWalletAddress(user));
                        userRewardWalletRepository.save(wallet);
                    }
                },
                () -> userRewardWalletRepository.save(UserRewardWallet.builder()
                        .userId(user.getUserId())
                        .pointBalance(0L)
                        .daeguDid(user.getDaeguDid())
                        .walletAddress(createWalletAddress(user))
                        .build())
        );
    }

    private String createWalletAddress(User user) {
        try {
            DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> account =
                    daeguChainAccountService.createAccount(new DaeguChainDto.AccountCreateRequest(null, null));
            if (account == null
                    || account.getData() == null
                    || account.getData().getKeyPair() == null
                    || isBlank(account.getData().getKeyPair().getAddress())) {
                throw new BadRequestApiException("DaeguChain account address is empty");
            }
            return account.getData().getKeyPair().getAddress();
        } catch (RuntimeException exception) {
            if (isDevProfile() && isTokenRequiredError(exception)) {
                return buildLocalTestWalletAddress(user);
            }
            log.warn("Daegu wallet provisioning failed. userId={}", user.getUserId(), exception);
            return null;
        }
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

    private String buildLocalTestWalletAddress(User user) {
        long hash = Integer.toUnsignedLong(Objects.hash("soh-signup-wallet", user.getUserId()));
        return "0x" + "%040x".formatted(hash);
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev") || profile.equals("local"));
    }

    private boolean isTokenRequiredError(Throwable exception) {
        return exception.getMessage() != null && exception.getMessage().contains("token is required");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
