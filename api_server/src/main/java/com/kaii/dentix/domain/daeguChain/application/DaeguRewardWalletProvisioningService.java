package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DaeguRewardWalletProvisioningService {

    private static final String RECLAIM_ALLOWANCE = String.valueOf(Long.MAX_VALUE);
    private static final Pattern LEGACY_DID_PRIVATE_KEY = Pattern.compile("^(?:0x)?[0-9a-fA-F]{64}$");

    private final DaeguChainAccountService accountService;
    private final DaeguChainToken20Service token20Service;
    private final DaeguChainProperties properties;
    private final DaeguWalletPrivateKeyCipher privateKeyCipher;

    public ProvisionedWallet createActivatedWallet(Long userId) {
        DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> response = DaeguChainApiLogContext.withUser(
                userId,
                "리워드 지갑 생성",
                () -> accountService.createAccount(new DaeguChainDto.AccountCreateRequest(null, null))
        );
        DaeguChainDto.KeyPair keyPair = response == null || response.getData() == null
                ? null
                : response.getData().getKeyPair();
        String walletAddress = keyPair == null ? null : keyPair.getAddress();
        String walletPrivateKey = keyPair == null ? null : keyPair.getPrivatekey();
        if (isBlank(walletAddress)) {
            throw new BadRequestApiException("DaeguChain wallet address is empty");
        }
        if (isBlank(walletPrivateKey)) {
            throw new BadRequestApiException("DaeguChain wallet private key is empty");
        }

        activateWallet(userId, walletAddress);
        return new ProvisionedWallet(walletAddress, privateKeyCipher.encrypt(walletPrivateKey));
    }

    public String encryptWalletPrivateKey(String walletPrivateKey) {
        return privateKeyCipher.encrypt(walletPrivateKey);
    }

    public boolean requiresWalletReplacement(String walletPrivateKeyCiphertext) {
        if (isBlank(walletPrivateKeyCiphertext)) {
            return true;
        }
        String walletPrivateKey = privateKeyCipher.decrypt(walletPrivateKeyCiphertext).trim();
        return isLegacyDidPrivateKey(walletPrivateKey);
    }

    private void activateWallet(Long userId, String walletAddress) {
        DaeguChainDto.ApiResponse<JsonNode> response = DaeguChainApiLogContext.withUser(
                userId,
                "리워드 지갑 활성화",
                () -> accountService.faucet(new DaeguChainDto.AccountAddressRequest(null, null, walletAddress))
        );
        assertSuccessful("DaeguChain reward wallet activation failed", response);
    }

    public void approveRewardContract(
            Long userId,
            String contractAddress,
            String walletAddress,
            String walletPrivateKeyCiphertext
    ) {
        if (isBlank(contractAddress) || isBlank(walletAddress)) {
            throw new BadRequestApiException("reward token contract and wallet address are required");
        }
        if (isBlank(properties.getTokenOwnerAddress())) {
            throw new BadRequestApiException("token owner address is not configured");
        }
        String walletPrivateKey = privateKeyCipher.decrypt(walletPrivateKeyCiphertext);
        if (isLegacyDidPrivateKey(walletPrivateKey.trim())) {
            throw new BadRequestApiException(
                    "legacy DID key cannot approve reward tokens; reward wallet replacement is required"
            );
        }
        DaeguChainDto.ApiResponse<JsonNode> response = DaeguChainApiLogContext.withUser(
                userId,
                "리워드 지갑 회수 권한 승인",
                () -> token20Service.approveToken(new DaeguChainDto.TokenApproveRequest(
                        null,
                        null,
                        contractAddress,
                        walletAddress,
                        walletPrivateKey,
                        properties.getTokenOwnerAddress(),
                        RECLAIM_ALLOWANCE
                ))
        );
        assertSuccessful("DaeguChain reward reclaim approval failed", response);
    }

    private void assertSuccessful(String action, DaeguChainDto.ApiResponse<?> response) {
        if (response != null && !isFailedState(response.getState())) {
            return;
        }
        StringBuilder message = new StringBuilder(action);
        if (response == null) {
            message.append(". response is empty");
        } else {
            if (!isBlank(response.getState())) {
                message.append(". state=").append(response.getState());
            }
            if (!isBlank(response.getMsg())) {
                message.append(", msg=").append(response.getMsg());
            }
            if (response.getRcode() != null && !response.getRcode().isEmpty()) {
                message.append(", rcode=").append(response.getRcode());
            }
        }
        throw new BadRequestApiException(message.toString());
    }

    private boolean isFailedState(String state) {
        return "ERROR".equalsIgnoreCase(state)
                || "OOPS".equalsIgnoreCase(state)
                || "FAIL".equalsIgnoreCase(state)
                || "FAILED".equalsIgnoreCase(state);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isLegacyDidPrivateKey(String walletPrivateKey) {
        return LEGACY_DID_PRIVATE_KEY.matcher(walletPrivateKey).matches();
    }

    public record ProvisionedWallet(String walletAddress, String privateKeyCiphertext) {
    }
}
