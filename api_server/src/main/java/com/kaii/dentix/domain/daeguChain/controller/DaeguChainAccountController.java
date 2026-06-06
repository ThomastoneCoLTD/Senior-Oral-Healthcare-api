package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
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
@RequestMapping("/daegu-chain/common/account")
public class DaeguChainAccountController {

    private final DaeguChainAccountService daeguChainAccountService;

    @PostMapping("/create")
    public DataResponse<DaeguChainDto.ApiResponse<DaeguChainDto.KeyPairData>> createAccount(
            @RequestBody(required = false) DaeguChainDto.AccountCreateRequest request
    ) {
        return new DataResponse<>(daeguChainAccountService.createAccount(resolve(request)));
    }

    @PostMapping("/faucet")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> faucet(
            @Valid @RequestBody DaeguChainDto.AccountAddressRequest request
    ) {
        return new DataResponse<>(daeguChainAccountService.faucet(request));
    }

    @PostMapping("/balance")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getBalance(
            @Valid @RequestBody DaeguChainDto.AccountAddressRequest request
    ) {
        return new DataResponse<>(daeguChainAccountService.getBalance(request));
    }

    @PostMapping("/information")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getInformation(
            @Valid @RequestBody DaeguChainDto.AccountAddressRequest request
    ) {
        return new DataResponse<>(daeguChainAccountService.getInformation(request));
    }

    @PostMapping("/transfer")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> transfer(
            @Valid @RequestBody DaeguChainDto.AccountTransferRequest request
    ) {
        return new DataResponse<>(daeguChainAccountService.transfer(request));
    }

    private DaeguChainDto.AccountCreateRequest resolve(DaeguChainDto.AccountCreateRequest request) {
        return request == null ? new DaeguChainDto.AccountCreateRequest(null, null) : request;
    }
}
