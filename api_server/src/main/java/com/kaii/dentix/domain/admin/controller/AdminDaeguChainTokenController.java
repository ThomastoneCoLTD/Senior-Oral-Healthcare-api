package com.kaii.dentix.domain.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.admin.application.AdminDaeguChainTokenService;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/daegu-chain/token")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminDaeguChainTokenController {

    private final AdminDaeguChainTokenService adminDaeguChainTokenService;

    @PostMapping("/options")
    public DataResponse<List<String>> getTokenNames() {
        return new DataResponse<>(adminDaeguChainTokenService.getTokenNames());
    }

    @PostMapping("/list")
    public DataResponse<JsonNode> getTokenList() {
        return new DataResponse<>(adminDaeguChainTokenService.getTokenList());
    }

    @PostMapping("/create")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> createToken(
            @Valid @RequestBody AdminDaeguChainTokenDto.CreateRequest request
    ) {
        return new DataResponse<>(adminDaeguChainTokenService.createToken(request));
    }
}
