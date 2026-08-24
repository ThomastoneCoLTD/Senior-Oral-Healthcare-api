package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiLogContext;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainToken20Service;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
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
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDaeguProvisioningService {

    private static final String RECLAIM_ALLOWANCE = Long.MAX_VALUE + "";

    private final DaeguChainDidService daeguChainDidService;
    private final DaeguChainAccountService daeguChainAccountService;
    private final DaeguChainToken20Service daeguChainToken20Service;
    private final DaeguChainProperties daeguChainProperties;
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
                        walletAddress = resolveWalletAddress(user.getUserId(), null);
                    }
                    wallet.updateDaeguWallet(externalDid, walletAddress);
                    userRewardWalletRepository.save(wallet);
                    return walletAddress;
                })
                .orElseGet(() -> {
                    String walletAddress = resolveWalletAddress(user.getUserId(), null);
                    userRewardWalletRepository.save(UserRewardWallet.builder()
                            .userId(user.getUserId())
                            .pointBalance(0L)
                            .daeguDid(externalDid)
                            .walletAddress(walletAddress)
                            .build());
                    return walletAddress;
                });
    }

    public String ensureProvisioned(User user) {
        if (user == null || user.getUserId() == null) {
            throw new BadRequestApiException("User is required for Daegu provisioning");
        }

        String walletAddress = null;
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED || isBlank(user.getDaeguDid())) {
            walletAddress = provisionDid(user);
        }
        if (user.getDaeguDidStatus() != UserDaeguIdentityStatus.ISSUED) {
            throw new BadRequestApiException("Daegu DID provisioning failed");
        }
        return provisionWallet(user, walletAddress);
    }

    private String provisionDid(User user) {
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
            if (isBlank(did)) {
                throw new BadRequestApiException("DaeguChain DID is empty");
            }
            user.updateDaeguDid(did, key, UserDaeguIdentityStatus.ISSUED);
            return walletAddress;
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

    private String provisionWallet(User user, String didWalletAddress) {
        return userRewardWalletRepository.findByUserId(user.getUserId())
                .map(wallet -> {
                    String daeguDid = user.getDaeguDid();
                    if (!isBlank(wallet.getWalletAddress())) {
                        wallet.updateDaeguWallet(daeguDid, wallet.getWalletAddress());
                        userRewardWalletRepository.save(wallet);
                        return wallet.getWalletAddress();
                    }
                    String walletAddress = resolveWalletAddress(user.getUserId(), didWalletAddress);
                    wallet.updateDaeguWallet(daeguDid, walletAddress);
                    userRewardWalletRepository.save(wallet);
                    return walletAddress;
                })
                .orElseGet(() -> {
                    String walletAddress = resolveWalletAddress(user.getUserId(), didWalletAddress);
                    userRewardWalletRepository.save(UserRewardWallet.builder()
                            .userId(user.getUserId())
                            .pointBalance(0L)
                            .daeguDid(user.getDaeguDid())
                            .walletAddress(walletAddress)
                            .build());
                    return walletAddress;
                });
    }

    private String resolveWalletAddress(Long userId, String didWalletAddress) {
        if (!isBlank(didWalletAddress)) {
            return didWalletAddress;
        }
        return createDaeguWalletAddress(userId);
    }

    private String createDaeguWalletAddress(Long userId) {
        DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> response = DaeguChainApiLogContext.withUser(
                userId,
                "리워드 지갑 생성",
                () -> daeguChainAccountService.createAccount(new DaeguChainDto.AccountCreateRequest(null, null))
        );
        DaeguChainDto.KeyPair keyPair = response == null || response.getData() == null
                ? null
                : response.getData().getKeyPair();
        String walletAddress = keyPair == null ? null : keyPair.getAddress();
        if (isBlank(walletAddress)) {
            throw new BadRequestApiException("DaeguChain wallet address is empty");
        }
        if (isBlank(keyPair.getPrivatekey())) {
            throw new BadRequestApiException("DaeguChain wallet private key is empty");
        }
        approveRewardReclaim(userId, walletAddress, keyPair.getPrivatekey());
        return walletAddress;
    }

    private void approveRewardReclaim(Long userId, String walletAddress, String walletPrivateKey) {
        if (daeguChainProperties.getRewardTokenContracts() == null
                || daeguChainProperties.getRewardTokenContracts().isEmpty()) {
            return;
        }
        if (isBlank(daeguChainProperties.getTokenOwnerAddress())) {
            throw new BadRequestApiException("token owner address is not configured");
        }

        Set<String> contractAddresses = new LinkedHashSet<>(
                daeguChainProperties.getRewardTokenContracts().values()
        );
        for (String contractAddress : contractAddresses) {
            if (isBlank(contractAddress)) {
                continue;
            }
            DaeguChainDto.ApiResponse<JsonNode> response = DaeguChainApiLogContext.withUser(
                    userId,
                    "리워드 지갑 회수 권한 승인",
                    () -> daeguChainToken20Service.approveToken(new DaeguChainDto.TokenApproveRequest(
                            null,
                            null,
                            contractAddress,
                            walletAddress,
                            walletPrivateKey,
                            daeguChainProperties.getTokenOwnerAddress(),
                            RECLAIM_ALLOWANCE
                    ))
            );
            if (isFailedResponse(response)) {
                throw new BadRequestApiException(buildApprovalFailureMessage(response));
            }
        }
    }

    private String buildApprovalFailureMessage(DaeguChainDto.ApiResponse<?> response) {
        StringBuilder message = new StringBuilder("DaeguChain reward reclaim approval failed");
        if (!isBlank(response.getState())) {
            message.append(". state=").append(response.getState());
        }
        if (!isBlank(response.getMsg())) {
            message.append(", msg=").append(response.getMsg());
        }
        if (response.getRcode() != null && !response.getRcode().isEmpty()) {
            message.append(", rcode=").append(response.getRcode());
        }
        return message.toString();
    }

    private boolean isFailedResponse(DaeguChainDto.ApiResponse<?> response) {
        if (response == null || isBlank(response.getState())) {
            return false;
        }
        String state = response.getState();
        return "ERROR".equalsIgnoreCase(state)
                || "OOPS".equalsIgnoreCase(state)
                || "FAIL".equalsIgnoreCase(state)
                || "FAILED".equalsIgnoreCase(state);
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
