package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DaeguChainBasicService {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<DaeguChainDto.RpcNodeData> getRpcNode(DaeguChainDto.BasicRequest request) {
        return daeguChainClient.getRpcNode(
                DaeguChainDto.ChainRequest.builder()
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<DaeguChainDto.ChainIdData> getChainName(DaeguChainDto.BasicRequest request) {
        return daeguChainClient.getChainName(
                DaeguChainDto.ChainRequest.builder()
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<DaeguChainDto.NodeInfoData> getNodeInfo(DaeguChainDto.NodeInfoRequest request) {
        return daeguChainClient.getNodeInfo(
                DaeguChainDto.TokenChainRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
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
