package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DaeguChainToken20Service {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<JsonNode> createToken(DaeguChainDto.TokenCreateRequest request) {
        return daeguChainClient.createToken(
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

    public DaeguChainDto.ApiResponse<JsonNode> uploadToken(String token, String contAddr, MultipartFile tokenFile) {
        if (contAddr == null || contAddr.isBlank()) {
            throw new BadRequestApiException("contAddr is required");
        }
        if (tokenFile == null || tokenFile.isEmpty()) {
            throw new BadRequestApiException("tokenFile is required");
        }
        return daeguChainClient.uploadToken(resolveToken(token), contAddr, tokenFile);
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenList(DaeguChainDto.TokenListRequest request) {
        return daeguChainClient.getTokenList(
                DaeguChainDto.TokenListApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenSupply(DaeguChainDto.TokenContractRequest request) {
        return daeguChainClient.getTokenSupply(toContractApiRequest(request));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenBalance(DaeguChainDto.TokenBalanceRequest request) {
        return daeguChainClient.getTokenBalance(
                DaeguChainDto.TokenBalanceApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .addr(request.getAddr())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> mintToken(DaeguChainDto.TokenMintRequest request) {
        return daeguChainClient.mintToken(
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

    public DaeguChainDto.ApiResponse<JsonNode> burnToken(DaeguChainDto.TokenBurnRequest request) {
        return daeguChainClient.burnToken(
                DaeguChainDto.TokenBurnApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .holder(request.getHolder())
                        .holderPkey(request.getHolderPkey())
                        .amount(request.getAmount())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> approveToken(DaeguChainDto.TokenApproveRequest request) {
        return daeguChainClient.approveToken(
                DaeguChainDto.TokenApproveApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .holder(request.getHolder())
                        .holderPkey(request.getHolderPkey())
                        .approved(request.getApproved())
                        .amount(request.getAmount())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> getTokenAllowance(DaeguChainDto.TokenAllowanceRequest request) {
        return daeguChainClient.getTokenAllowance(
                DaeguChainDto.TokenAllowanceApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .holder(request.getHolder())
                        .spender(request.getSpender())
                        .build()
        );
    }

    public DaeguChainDto.ApiResponse<JsonNode> transferToken(DaeguChainDto.TokenTransferRequest request) {
        return daeguChainClient.transferToken(
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

    public DaeguChainDto.ApiResponse<JsonNode> transferTokenFrom(DaeguChainDto.TokenTransferFromRequest request) {
        return daeguChainClient.transferTokenFrom(
                DaeguChainDto.TokenTransferFromApiRequest.builder()
                        .token(resolveToken(request.getToken()))
                        .chain(resolveChain(request.getChain()))
                        .contAddr(request.getContAddr())
                        .sender(request.getSender())
                        .senderPkey(request.getSenderPkey())
                        .holder(request.getHolder())
                        .receiver(request.getReceiver())
                        .amount(request.getAmount())
                        .build()
        );
    }

    private DaeguChainDto.TokenContractApiRequest toContractApiRequest(DaeguChainDto.TokenContractRequest request) {
        return DaeguChainDto.TokenContractApiRequest.builder()
                .token(resolveToken(request.getToken()))
                .chain(resolveChain(request.getChain()))
                .contAddr(request.getContAddr())
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
