package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DaeguChainNft721Service {

    private final DaeguChainClient daeguChainClient;
    private final DaeguChainProperties properties;

    public DaeguChainDto.ApiResponse<JsonNode> uploadNft(String token, String description, MultipartFile nftFile) {
        if (nftFile == null || nftFile.isEmpty()) {
            throw new BadRequestApiException("nftFile is required");
        }
        return daeguChainClient.uploadNft(resolveToken(token), description, nftFile);
    }

    public DaeguChainDto.ApiResponse<JsonNode> create(Map<String, Object> request) {
        return call("/mitum/nft/create", request, List.of("owner_addr", "owner_pkey", "nft_name", "nft_uri", "royalty"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> list(Map<String, Object> request) {
        return call("/mitum/nft/nfts", request, List.of());
    }

    public DaeguChainDto.ApiResponse<JsonNode> collectionInfo(Map<String, Object> request) {
        return call("/mitum/nft/collec_info", request, List.of("cont_addr"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> setPolicy(Map<String, Object> request) {
        return call("/mitum/nft/set_policy", request, List.of("cont_addr", "owner_addr", "owner_pkey", "nft_name", "nft_uri", "royalty"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> mint(Map<String, Object> request) {
        return call("/mitum/nft/mint", request, List.of("cont_addr", "owner_addr", "owner_pkey", "receiver", "uri", "hash", "creator"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> mintForMultiCreator(Map<String, Object> request) {
        return call("/mitum/nft/mint_multi", request, List.of("cont_addr", "owner_addr", "owner_pkey", "receiver", "uri", "hash", "creators"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> multiMint(Map<String, Object> request) {
        return call("/mitum/nft/multi_mint", request, List.of("cont_addr", "owner_addr", "owner_pkey", "receiver", "number", "uri", "hash", "creator"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> nftId(Map<String, Object> request) {
        return call("/mitum/nft/nft_idx", request, List.of("cont_addr", "fact_hash"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> totalSupply(Map<String, Object> request) {
        return call("/mitum/nft/tot_supply", request, List.of("cont_addr"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> ownerOf(Map<String, Object> request) {
        return call("/mitum/nft/owner_of", request, List.of("cont_addr", "nft_idx"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> tokenUri(Map<String, Object> request) {
        return call("/mitum/nft/uri", request, List.of("cont_addr", "nft_idx"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> tokenInfo(Map<String, Object> request) {
        return call("/mitum/nft/token_info", request, List.of("cont_addr", "nft_idx"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> approve(Map<String, Object> request) {
        return call("/mitum/nft/approve", request, List.of("cont_addr", "holder", "holder_pkey", "operator", "nft_idx"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> getApproved(Map<String, Object> request) {
        return call("/mitum/nft/get_approved", request, List.of("cont_addr", "nft_idx"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> setApprovalForAll(Map<String, Object> request) {
        return call("/mitum/nft/set_appr_all", request, List.of("cont_addr", "holder", "holder_pkey", "operator", "mode"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> isApprovedForAll(Map<String, Object> request) {
        return call("/mitum/nft/is_appr_all", request, List.of("cont_addr", "holder"));
    }

    public DaeguChainDto.ApiResponse<JsonNode> transfer(Map<String, Object> request) {
        return call("/mitum/nft/transfer", request, List.of("cont_addr", "sender", "sender_pkey", "receiver", "nft_idx"));
    }

    private DaeguChainDto.ApiResponse<JsonNode> call(String path, Map<String, Object> request, List<String> requiredFields) {
        Map<String, Object> body = new LinkedHashMap<>(request == null ? Map.of() : request);
        body.putIfAbsent("token", resolveToken((String) body.get("token")));
        body.putIfAbsent("chain", resolveChain((String) body.get("chain")));
        validateRequiredFields(body, requiredFields);
        return daeguChainClient.postNft(path, body);
    }

    private void validateRequiredFields(Map<String, Object> body, List<String> requiredFields) {
        for (String field : requiredFields) {
            Object value = body.get(field);
            if (value == null || (value instanceof String stringValue && stringValue.isBlank())) {
                throw new BadRequestApiException(field + " is required");
            }
        }
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
