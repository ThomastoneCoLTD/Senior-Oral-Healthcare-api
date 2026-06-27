package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainPointService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/daegu-chain/point")
public class DaeguChainPointController {

    private final DaeguChainPointService daeguChainPointService;

    @PostMapping("/create")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> createPoint(
            @Valid @RequestBody DaeguChainDto.TokenCreateRequest request
    ) {
        return new DataResponse<>(daeguChainPointService.createPoint(request));
    }

    @PostMapping("/list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getPointList(
            @RequestBody(required = false) DaeguChainDto.TokenListRequest request
    ) {
        return new DataResponse<>(daeguChainPointService.getPointList(resolve(request)));
    }

    @PostMapping("/balance")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getPointBalance(
            @Valid @RequestBody DaeguChainDto.TokenBalanceRequest request
    ) {
        return new DataResponse<>(daeguChainPointService.getPointBalance(request));
    }

    @PostMapping("/mint")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> mintPoint(
            @Valid @RequestBody DaeguChainDto.TokenMintRequest request
    ) {
        return new DataResponse<>(daeguChainPointService.mintPoint(request));
    }

    @PostMapping("/transfer")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> transferPoint(
            @Valid @RequestBody DaeguChainDto.TokenTransferRequest request
    ) {
        return new DataResponse<>(daeguChainPointService.transferPoint(request));
    }

    private DaeguChainDto.TokenListRequest resolve(DaeguChainDto.TokenListRequest request) {
        return request == null ? new DaeguChainDto.TokenListRequest(null, null) : request;
    }
}
