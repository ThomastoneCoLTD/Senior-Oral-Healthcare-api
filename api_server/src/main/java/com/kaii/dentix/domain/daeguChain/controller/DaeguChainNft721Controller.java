package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainNft721Service;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/daegu-chain/nft-721")
public class DaeguChainNft721Controller {

    private final DaeguChainNft721Service daeguChainNft721Service;

    @PostMapping("/file-upload")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> uploadNft(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String description,
            @RequestPart("nftFile") MultipartFile nftFile
    ) {
        return new DataResponse<>(daeguChainNft721Service.uploadNft(token, description, nftFile));
    }

    @PostMapping("/create")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> create(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.create(request));
    }

    @PostMapping("/list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> list(@RequestBody(required = false) Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.list(request));
    }

    @PostMapping("/collection-info")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> collectionInfo(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.collectionInfo(request));
    }

    @PostMapping("/set-policy")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> setPolicy(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.setPolicy(request));
    }

    @PostMapping("/mint")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> mint(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.mint(request));
    }

    @PostMapping("/mint-for-multi-creator")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> mintForMultiCreator(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.mintForMultiCreator(request));
    }

    @PostMapping("/multi-mint")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> multiMint(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.multiMint(request));
    }

    @PostMapping("/nft-id")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> nftId(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.nftId(request));
    }

    @PostMapping("/total-supply")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> totalSupply(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.totalSupply(request));
    }

    @PostMapping("/owner-of")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> ownerOf(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.ownerOf(request));
    }

    @PostMapping("/token-uri")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> tokenUri(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.tokenUri(request));
    }

    @PostMapping("/token-info")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> tokenInfo(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.tokenInfo(request));
    }

    @PostMapping("/approve")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> approve(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.approve(request));
    }

    @PostMapping("/get-approved")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getApproved(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.getApproved(request));
    }

    @PostMapping("/set-approval-for-all")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> setApprovalForAll(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.setApprovalForAll(request));
    }

    @PostMapping("/is-approved-for-all")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> isApprovedForAll(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.isApprovedForAll(request));
    }

    @PostMapping("/transfer")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> transfer(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainNft721Service.transfer(request));
    }
}
