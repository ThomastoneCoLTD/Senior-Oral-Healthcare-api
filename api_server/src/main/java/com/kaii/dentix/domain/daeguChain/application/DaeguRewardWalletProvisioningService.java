package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DaeguRewardWalletProvisioningService {

    private static final String RECLAIM_ALLOWANCE = String.valueOf(Long.MAX_VALUE);
    private static final int ACTIVATION_RETRY_COUNT = 6;
    private static final long ACTIVATION_RETRY_DELAY_MILLIS = 500L;

    private final DaeguChainAccountService accountService;
    private final DaeguChainToken20Service token20Service;
    private final DaeguChainProperties properties;

    public String createActivatedWallet(Long userId) {
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
        approveRewardReclaim(userId, walletAddress, walletPrivateKey);
        return walletAddress;
    }

    public void approveActivatedWallet(Long userId, String walletAddress, String walletPrivateKey) {
        if (isBlank(walletAddress) || isBlank(walletPrivateKey)) {
            return;
        }
        approveRewardReclaim(userId, walletAddress, walletPrivateKey);
    }

    private void activateWallet(Long userId, String walletAddress) {
        DaeguChainDto.ApiResponse<JsonNode> response = DaeguChainApiLogContext.withUser(
                userId,
                "리워드 지갑 활성화",
                () -> accountService.faucet(new DaeguChainDto.AccountAddressRequest(null, null, walletAddress))
        );
        assertSuccessful("DaeguChain reward wallet activation failed", response);
    }

    private void approveRewardReclaim(Long userId, String walletAddress, String walletPrivateKey) {
        if (properties.getRewardTokenContracts() == null || properties.getRewardTokenContracts().isEmpty()) {
            return;
        }
        if (isBlank(properties.getTokenOwnerAddress())) {
            throw new BadRequestApiException("token owner address is not configured");
        }

        Set<String> contractAddresses = new LinkedHashSet<>(properties.getRewardTokenContracts().values());
        for (String contractAddress : contractAddresses) {
            if (isBlank(contractAddress)) {
                continue;
            }
            approveWithActivationRetry(userId, new DaeguChainDto.TokenApproveRequest(
                    null,
                    null,
                    contractAddress,
                    walletAddress,
                    walletPrivateKey,
                    properties.getTokenOwnerAddress(),
                    RECLAIM_ALLOWANCE
            ));
        }
    }

    private void approveWithActivationRetry(Long userId, DaeguChainDto.TokenApproveRequest request) {
        for (int attempt = 1; attempt <= ACTIVATION_RETRY_COUNT; attempt++) {
            try {
                DaeguChainDto.ApiResponse<JsonNode> response = DaeguChainApiLogContext.withUser(
                        userId,
                        "리워드 지갑 회수 권한 승인",
                        () -> token20Service.approveToken(request)
                );
                assertSuccessful("DaeguChain reward reclaim approval failed", response);
                return;
            } catch (BadRequestApiException exception) {
                if (attempt == ACTIVATION_RETRY_COUNT || !isActivationPending(exception)) {
                    throw exception;
                }
                waitForActivation();
            }
        }
    }

    private boolean isActivationPending(BadRequestApiException exception) {
        String message = exception.getMessage();
        if (isBlank(message)) {
            return false;
        }
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("p06d502")
                || normalized.contains("account not found")
                || normalized.contains("sender account");
    }

    private void waitForActivation() {
        try {
            Thread.sleep(ACTIVATION_RETRY_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BadRequestApiException("Interrupted while waiting for DaeguChain wallet activation");
        }
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
}
