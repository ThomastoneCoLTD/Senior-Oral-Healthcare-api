package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/daegu-chain/did")
public class DaeguChainDidController {

    private final DaeguChainDidService daeguChainDidService;

    @PostMapping("/project-list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> projectList(@RequestBody(required = false) Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.projectList(request));
    }

    @PostMapping("/regist-project")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> registProject(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.registProject(request));
    }

    @PostMapping("/template-list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> templateList(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.templateList(request));
    }

    @PostMapping("/edit-template")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> editTemplate(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.editTemplate(request));
    }

    @PostMapping("/account-list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> accountList(@RequestBody(required = false) Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.accountList(request));
    }

    @PostMapping("/account-create")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> createAccount(@RequestBody(required = false) Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.createAccount(request));
    }

    @PostMapping("/get-key")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getKey(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.getKey(request));
    }

    @PostMapping("/issue-credential")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> issueCredential(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.issueCredential(request));
    }

    @PostMapping("/disclosure")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> disclosure(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.disclosure(request));
    }

    @PostMapping("/verification")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> verification(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.verification(request));
    }

    @PostMapping("/revoke-credential")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> revokeCredential(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.revokeCredential(request));
    }

    @PostMapping("/qr-code")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> qrCode(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainDidService.qrCode(request));
    }
}
