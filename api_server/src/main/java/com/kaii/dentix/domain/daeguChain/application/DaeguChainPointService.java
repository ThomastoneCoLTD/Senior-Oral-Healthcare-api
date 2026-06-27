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
public class DaeguChainPointService {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<JsonNode> createPoint(DaeguChainDto.TokenCreateRequest request) {
        return daeguChainClient.createPoint(
                DaeguChainDto.TokenCreateApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .chainName(request.getChainName())
                        .ownerAddr(request.getOwnerAddr())
                        .ownerPkey(request.getOwnerPkey())
                        .tokenName(request.getTokenName())
                        .tokenSymbol(request.getTokenSymbol())
                        .decimals(request.getDecimals())
                        .supply(request.getSupply())
                        .mintable(request.getMintable())
                        .lockable(request.getLockable())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getPointList(DaeguChainDto.TokenListRequest request) {
        return daeguChainClient.getPointList(
                DaeguChainDto.TokenListApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getPointBalance(DaeguChainDto.TokenBalanceRequest request) {
        return daeguChainClient.getPointBalance(
                DaeguChainDto.TokenBalanceApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .addr(request.getAddr())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> mintPoint(DaeguChainDto.TokenMintRequest request) {
        return daeguChainClient.mintPoint(
                DaeguChainDto.TokenMintApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .owner(request.getOwner())
                        .ownerPkey(request.getOwnerPkey())
                        .receiver(request.getReceiver())
                        .amount(request.getAmount())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> transferPoint(DaeguChainDto.TokenTransferRequest request) {
        return daeguChainClient.transferPoint(
                DaeguChainDto.TokenTransferApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .sender(request.getSender())
                        .senderPkey(request.getSenderPkey())
                        .receiver(request.getReceiver())
                        .amount(request.getAmount())
                        .build()
        );
    }

    private String resolveChain(String chain) {
        return chain == null || chain.isBlank() ? properties.getChain() : chain;
    }

    private String resolveToken(String token) {
        String configuredAppKey = properties.resolveAppKey();
        String resolvedToken = configuredAppKey == null || configuredAppKey.isBlank() ? token : configuredAppKey;
        if (resolvedToken == null || resolvedToken.isBlank()) {
            throw new BadRequestApiException("token is required");
        }
        return resolvedToken;
    }
}
