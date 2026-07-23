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
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDaeguProvisioningService {

    private final DaeguChainDidService daeguChainDidService;
    private final DaeguChainAccountService daeguChainAccountService;
    private final UserRewardWalletRepository userRewardWalletRepository;

    public String provisionForSignUp(User user) {
        String walletAddress = provisionDid(user);
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED) {
            return null;
        }
        return provisionWalletForSignUp(user, walletAddress);
    }

    private String provisionDid(User user) {
        try {
            DaeguChainDto.ApiResponse<JsonNode> response = daeguChainDidService.createAccount(buildDidCreateRequest(user));
            JsonNode data = response.getData();
            String did = findFirstText(data, "did", "DID", "account");
            String key = findFirstText(data, "publickey", "public_key", "publicKey", "key_id", "keyId");
            String walletAddress = findFirstText(
                    data,
                    "walletAddress",
                    "wallet_address",
                    "accountAddress",
                    "account_address",
                    "address"
            );
            if (isBlank(did)) {
                throw new BadRequestApiException("DaeguChain DID is empty");
            }
            user.updateDaeguDid(did, key, UserDaeguIdentityStatus.ISSUED);
            return walletAddress;
        } catch (RuntimeException exception) {
            log.warn("Daegu DID provisioning failed. userId={}", user.getUserId(), exception);
            user.updateDaeguDid(null, null, UserDaeguIdentityStatus.FAILED);
            return null;
        }
    }

    private String provisionWalletForSignUp(User user, String didWalletAddress) {
        try {
            return provisionWallet(user, didWalletAddress);
        } catch (RuntimeException exception) {
            log.warn("Daegu DID wallet provisioning failed. userId={}", user.getUserId(), exception);
            return null;
        }
    }

    private Map<String, Object> buildDidCreateRequest(User user) {
        if (user == null || isBlank(user.getUserLoginIdentifier())) {
            return Map.of();
        }
        return Map.of(
                "label", user.getUserLoginIdentifier()
        );
    }

    private String provisionWallet(User user, String didWalletAddress) {
        return userRewardWalletRepository.findByUserId(user.getUserId())
                .map(wallet -> {
                    String daeguDid = user.getDaeguDid();
                    if (!isBlank(wallet.getWalletAddress())) {
                        wallet.updateDaeguWallet(daeguDid, wallet.getWalletAddress());
                        userRewardWalletRepository.save(wallet);
                        return wallet.getWalletAddress();
                    }
                    String walletAddress = resolveWalletAddress(didWalletAddress);
                    wallet.updateDaeguWallet(daeguDid, walletAddress);
                    userRewardWalletRepository.save(wallet);
                    return walletAddress;
                })
                .orElseGet(() -> {
                    String walletAddress = resolveWalletAddress(didWalletAddress);
                    userRewardWalletRepository.save(UserRewardWallet.builder()
                            .userId(user.getUserId())
                            .pointBalance(0L)
                            .daeguDid(user.getDaeguDid())
                            .walletAddress(walletAddress)
                            .build());
                    return walletAddress;
                });
    }

    private String resolveWalletAddress(String didWalletAddress) {
        if (!isBlank(didWalletAddress)) {
            return didWalletAddress;
        }
        return createDaeguWalletAddress();
    }

    private String createDaeguWalletAddress() {
        DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> response =
                daeguChainAccountService.createAccount(new DaeguChainDto.AccountCreateRequest(null, null));
        String walletAddress = response == null
                || response.getData() == null
                || response.getData().getKeyPair() == null
                ? null
                : response.getData().getKeyPair().getAddress();
        if (isBlank(walletAddress)) {
            throw new BadRequestApiException("DaeguChain wallet address is empty");
        }
        return walletAddress;
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
