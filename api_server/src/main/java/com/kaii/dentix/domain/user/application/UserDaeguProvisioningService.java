package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiLogContext;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.application.DaeguRewardWalletProvisioningService;
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
    private final DaeguRewardWalletProvisioningService rewardWalletProvisioningService;
    private final UserRewardWalletRepository userRewardWalletRepository;

    public String provisionForSignUp(User user) {
        return ensureProvisioned(user);
    }

    /**
     * 다대구 인증으로 전달받은 DID를 리워드 지갑의 활성 DID로 연결한다.
     * 기존 로컬 사용자는 자체 DID와 지갑 주소를 유지하고, 토큰 처리에 사용할 DID만 다대구 DID로 바꾼다.
     */
    public String provisionForDadaegu(User user, String externalDid) {
        if (user == null || user.getUserId() == null) {
            throw new BadRequestApiException("User is required for Daegu provisioning");
        }
        if (isBlank(externalDid)) {
            throw new BadRequestApiException("DaDaegu DID is required");
        }

        if (isBlank(user.getDaeguDid())) {
            user.updateDaeguDid(externalDid, null, UserDaeguIdentityStatus.ISSUED);
        }

        return userRewardWalletRepository.findByUserId(user.getUserId())
                .map(wallet -> {
                    String walletAddress = wallet.getWalletAddress();
                    if (isBlank(walletAddress)) {
                        WalletProvision provision = resolveWallet(user.getUserId(), null);
                        walletAddress = provision.walletAddress();
                        wallet.updateWalletPrivateKeyCiphertext(provision.privateKeyCiphertext());
                    }
                    wallet.updateDaeguWallet(externalDid, walletAddress);
                    userRewardWalletRepository.save(wallet);
                    return walletAddress;
                })
                .orElseGet(() -> {
                    WalletProvision provision = resolveWallet(user.getUserId(), null);
                    String walletAddress = provision.walletAddress();
                    userRewardWalletRepository.save(UserRewardWallet.builder()
                            .userId(user.getUserId())
                            .pointBalance(0L)
                            .daeguDid(externalDid)
                            .walletAddress(walletAddress)
                            .walletPrivateKeyCiphertext(provision.privateKeyCiphertext())
                            .build());
                    return walletAddress;
                });
    }

    public String ensureProvisioned(User user) {
        if (user == null || user.getUserId() == null) {
            throw new BadRequestApiException("User is required for Daegu provisioning");
        }

        WalletProvision walletProvision = null;
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED || isBlank(user.getDaeguDid())) {
            walletProvision = provisionDid(user);
        }
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED) {
            throw new BadRequestApiException("Daegu DID provisioning failed");
        }
        return provisionWallet(user, walletProvision);
    }

    private WalletProvision provisionDid(User user) {
        try {
            DaeguChainDto.ApiResponse<JsonNode> response = DaeguChainApiLogContext.withUser(
                    user.getUserId(),
                    "DID 계정 발급",
                    () -> daeguChainDidService.createAccount(buildDidCreateRequest(user))
            );
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
            String walletPrivateKey = findFirstText(data, "privatekey", "private_key", "privateKey");
            if (isBlank(did)) {
                throw new BadRequestApiException("DaeguChain DID is empty");
            }
            String privateKeyCiphertext = rewardWalletProvisioningService.encryptWalletPrivateKey(walletPrivateKey);
            user.updateDaeguDid(did, key, UserDaeguIdentityStatus.ISSUED);
            return new WalletProvision(walletAddress, privateKeyCiphertext);
        } catch (RuntimeException exception) {
            log.warn("Daegu DID provisioning failed. userId={}", user.getUserId(), exception);
            user.updateDaeguDid(null, null, UserDaeguIdentityStatus.FAILED);
            throw new BadRequestApiException("Daegu DID provisioning failed");
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

    private String provisionWallet(User user, WalletProvision didWallet) {
        return userRewardWalletRepository.findByUserId(user.getUserId())
                .map(wallet -> {
                    String daeguDid = user.getDaeguDid();
                    if (!isBlank(wallet.getWalletAddress())) {
                        wallet.updateDaeguWallet(daeguDid, wallet.getWalletAddress());
                        userRewardWalletRepository.save(wallet);
                        return wallet.getWalletAddress();
                    }
                    WalletProvision provision = resolveWallet(user.getUserId(), didWallet);
                    String walletAddress = provision.walletAddress();
                    wallet.updateDaeguWallet(daeguDid, walletAddress);
                    wallet.updateWalletPrivateKeyCiphertext(provision.privateKeyCiphertext());
                    userRewardWalletRepository.save(wallet);
                    return walletAddress;
                })
                .orElseGet(() -> {
                    WalletProvision provision = resolveWallet(user.getUserId(), didWallet);
                    String walletAddress = provision.walletAddress();
                    userRewardWalletRepository.save(UserRewardWallet.builder()
                            .userId(user.getUserId())
                            .pointBalance(0L)
                            .daeguDid(user.getDaeguDid())
                            .walletAddress(walletAddress)
                            .walletPrivateKeyCiphertext(provision.privateKeyCiphertext())
                            .build());
                    return walletAddress;
                });
    }

    private WalletProvision resolveWallet(Long userId, WalletProvision didWallet) {
        if (didWallet != null && !isBlank(didWallet.walletAddress())) {
            return didWallet;
        }
        DaeguRewardWalletProvisioningService.ProvisionedWallet provisioned =
                rewardWalletProvisioningService.createActivatedWallet(userId);
        return new WalletProvision(provisioned.walletAddress(), provisioned.privateKeyCiphertext());
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

    private record WalletProvision(String walletAddress, String privateKeyCiphertext) {
    }
}
