package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainBlockService;
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
@RequestMapping("/daegu-chain/common/block")
public class DaeguChainBlockController {

    private final DaeguChainBlockService daeguChainBlockService;

    @PostMapping("/number")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getBlockNumber(
            @RequestBody(required = false) DaeguChainDto.BlockNumberRequest request
    ) {
        return new DataResponse<>(daeguChainBlockService.getBlockNumber(resolve(request)));
    }

    @PostMapping("/by-number")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getBlockByNumber(
            @Valid @RequestBody DaeguChainDto.BlockByNumberRequest request
    ) {
        return new DataResponse<>(daeguChainBlockService.getBlockByNumber(request));
    }

    @PostMapping("/by-hash")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getBlockByHash(
            @Valid @RequestBody DaeguChainDto.BlockByHashRequest request
    ) {
        return new DataResponse<>(daeguChainBlockService.getBlockByHash(request));
    }

    @PostMapping("/transaction-count")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTransactionCount(
            @Valid @RequestBody DaeguChainDto.BlockByNumberRequest request
    ) {
        return new DataResponse<>(daeguChainBlockService.getTransactionCount(request));
    }

    @PostMapping("/transaction")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTransaction(
            @Valid @RequestBody DaeguChainDto.TransactionRequest request
    ) {
        return new DataResponse<>(daeguChainBlockService.getTransaction(request));
    }

    private DaeguChainDto.BlockNumberRequest resolve(DaeguChainDto.BlockNumberRequest request) {
        return request == null ? new DaeguChainDto.BlockNumberRequest(null, null) : request;
    }
}
