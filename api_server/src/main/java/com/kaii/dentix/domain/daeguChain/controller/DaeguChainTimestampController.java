package com.kaii.dentix.domain.daeguChain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainTimestampService;
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
@RequestMapping("/daegu-chain/timestamp")
public class DaeguChainTimestampController {

    private final DaeguChainTimestampService daeguChainTimestampService;

    @PostMapping("/project-list")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> projectList(@RequestBody(required = false) Map<String, Object> request) {
        return new DataResponse<>(daeguChainTimestampService.projectList(request));
    }

    @PostMapping("/regist-project")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> registProject(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainTimestampService.registProject(request));
    }

    @PostMapping("/request")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> request(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainTimestampService.request(request));
    }

    @PostMapping("/get-key-by-hash")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getKeyByHash(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainTimestampService.getKeyByHash(request));
    }

    @PostMapping("/get-timestamp")
    public DataResponse<DaeguChainDto.ApiResponse<JsonNode>> getTimestamp(@RequestBody Map<String, Object> request) {
        return new DataResponse<>(daeguChainTimestampService.getTimestamp(request));
    }
}
