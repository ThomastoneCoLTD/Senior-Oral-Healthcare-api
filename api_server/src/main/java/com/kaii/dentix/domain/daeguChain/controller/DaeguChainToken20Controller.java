package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainToken20Service;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/daegu-chain/token-20")
public class DaeguChainToken20Controller {

    private final DaeguChainToken20Service daeguChainToken20Service;

    @PostMapping("/create")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> createToken(
            @Valid @RequestBody DaeguChainDto.TokenCreateRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.createToken(request));
    }

    @PostMapping("/upload")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> uploadToken(
            @RequestParam(required = false) String token,
            @RequestParam("contAddr") String contAddr,
            @RequestPart("tokenFile") MultipartFile tokenFile
    ) {
        return new DataResponse<>(daeguChainToken20Service.uploadToken(token, contAddr, tokenFile));
    }

    @PostMapping("/list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTokenList(
            @RequestBody(required = false) DaeguChainDto.TokenListRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.getTokenList(resolve(request)));
    }

    @PostMapping("/total-supply")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTokenSupply(
            @Valid @RequestBody DaeguChainDto.TokenContractRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.getTokenSupply(request));
    }

    @PostMapping("/balance")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTokenBalance(
            @Valid @RequestBody DaeguChainDto.TokenBalanceRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.getTokenBalance(request));
    }

    @PostMapping("/mint")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> mintToken(
            @Valid @RequestBody DaeguChainDto.TokenMintRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.mintToken(request));
    }

    @PostMapping("/burn")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> burnToken(
            @Valid @RequestBody DaeguChainDto.TokenBurnRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.burnToken(request));
    }

    @PostMapping("/approve")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> approveToken(
            @Valid @RequestBody DaeguChainDto.TokenApproveRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.approveToken(request));
    }

    @PostMapping("/allowance")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTokenAllowance(
            @Valid @RequestBody DaeguChainDto.TokenAllowanceRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.getTokenAllowance(request));
    }

    @PostMapping("/transfer")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> transferToken(
            @Valid @RequestBody DaeguChainDto.TokenTransferRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.transferToken(request));
    }

    @PostMapping("/transfer-from")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> transferTokenFrom(
            @Valid @RequestBody DaeguChainDto.TokenTransferFromRequest request
    ) {
        return new DataResponse<>(daeguChainToken20Service.transferTokenFrom(request));
    }

    private DaeguChainDto.TokenListRequest resolve(DaeguChainDto.TokenListRequest request) {
        return request == null ? new DaeguChainDto.TokenListRequest(null, null) : request;
    }
}
