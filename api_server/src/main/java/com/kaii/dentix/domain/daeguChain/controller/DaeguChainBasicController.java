package com.kaii.dentix.domain.daeguChain.controller;

import com.kaii.dentix.domain.daeguChain.application.DaeguChainBasicService;
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
@RequestMapping("/daegu-chain/common/basic")
public class DaeguChainBasicController {

    private final DaeguChainBasicService daeguChainBasicService;

    @PostMapping("/rpc-node")
    public DataResponse<DaeguChainDto.ApiResponse<DaeguChainDto.RpcNodeData>> getRpcNode(
            @RequestBody(required = false) DaeguChainDto.BasicRequest request
    ) {
        return new DataResponse<>(daeguChainBasicService.getRpcNode(resolve(request)));
    }

    @PostMapping("/chain-name")
    public DataResponse<DaeguChainDto.ApiResponse<DaeguChainDto.ChainIdData>> getChainName(
            @RequestBody(required = false) DaeguChainDto.BasicRequest request
    ) {
        return new DataResponse<>(daeguChainBasicService.getChainName(resolve(request)));
    }

    @PostMapping("/node-info")
    public DataResponse<DaeguChainDto.ApiResponse<DaeguChainDto.NodeInfoData>> getNodeInfo(
            @Valid @RequestBody DaeguChainDto.NodeInfoRequest request
    ) {
        return new DataResponse<>(daeguChainBasicService.getNodeInfo(request));
    }

    private DaeguChainDto.BasicRequest resolve(DaeguChainDto.BasicRequest request) {
        return request == null ? new DaeguChainDto.BasicRequest(null) : request;
    }
}
