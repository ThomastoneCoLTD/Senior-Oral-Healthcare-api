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
public class DaeguChainBlockService {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<JsonNode> getBlockNumber(DaeguChainDto.BlockNumberRequest request) {
        return daeguChainClient.getBlockNumber(
                DaeguChainDto.BlockNumberApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBlockByNumber(DaeguChainDto.BlockByNumberRequest request) {
        return daeguChainClient.getBlockByNumber(toBlockByNumberApiRequest(request));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getBlockByHash(DaeguChainDto.BlockByHashRequest request) {
        return daeguChainClient.getBlockByHash(
                DaeguChainDto.BlockByHashApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .blockHash(request.getBlockHash())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTransactionCount(DaeguChainDto.BlockByNumberRequest request) {
        return daeguChainClient.getTransactionCount(toBlockByNumberApiRequest(request));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTransaction(DaeguChainDto.TransactionRequest request) {
        return daeguChainClient.getTransaction(
                DaeguChainDto.TransactionApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .factHash(request.getFactHash())
                        .build()
        );
    }

    private DaeguChainDto.BlockByNumberApiRequest toBlockByNumberApiRequest(DaeguChainDto.BlockByNumberRequest request) {
        return DaeguChainDto.BlockByNumberApiRequest.builder()
                .token(resolveToken(request.getToken()))
                .chain(resolveChain(request.getChain()))
                .blockNum(request.getBlockNum())
                .build();
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
