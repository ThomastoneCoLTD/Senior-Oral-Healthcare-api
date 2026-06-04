package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DaeguChainAccountService {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData> createAccount(DaeguChainDto.AccountCreateRequest request) {
        return daeguChainClient.createAccount(
                DaeguChainDto.TokenChainRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> faucet(DaeguChainDto.AccountAddressRequest request) {
        return daeguChainClient.faucet(toAddressApiRequest(request));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBalance(DaeguChainDto.AccountAddressRequest request) {
        return daeguChainClient.getBalance(toAddressApiRequest(request));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getInformation(DaeguChainDto.AccountAddressRequest request) {
        return daeguChainClient.getInformation(toAddressApiRequest(request));
    }

    public DaeguChainDto.ApiResponse<JsonNode> transfer(DaeguChainDto.AccountTransferRequest request) {
        return daeguChainClient.transfer(
                DaeguChainDto.AccountTransferApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .privateKey(request.getPrivateKey())
                        .sender(request.getSender())
                        .receiver(request.getReceiver())
                        .amount(request.getAmount())
                        .build()
        );
    }

    private DaeguChainDto.AccountAddressApiRequest toAddressApiRequest(DaeguChainDto.AccountAddressRequest request) {
        return DaeguChainDto.AccountAddressApiRequest.builder()
                .token(resolveToken(request.getToken()))
                .chain(resolveChain(request.getChain()))
                .address(request.getAddress())
                .build();
    }

    private String resolveChain(String chain) {
        return chain == null || chain.isBlank() ? properties.getChain() : chain;
    }

    private String resolveToken(String token) {
        String resolvedToken = token == null || token.isBlank() ? properties.getToken() : token;
        if (resolvedToken == null || resolvedToken.isBlank()) {
            throw new BadRequestApiException("token is required");
        }
        return resolvedToken;
    }
}
