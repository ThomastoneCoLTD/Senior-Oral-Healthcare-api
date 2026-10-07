package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminRewardTransferRecoveryService;
import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.admin.dto.AdminRewardTransferRecoveryDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/daegu-chain/token/reward-transfers/{transactionId}/recovery")
public class AdminRewardTransferRecoveryController {
    private final AdminRewardTransferRecoveryService service;

    @PostMapping("/preview")
    public DataResponse<AdminRewardTransferRecoveryDto.Preview> preview(
            @PathVariable Long transactionId, @Valid @RequestBody AdminRewardTransferRecoveryDto.Request request) {
        return new DataResponse<>(service.preview(transactionId, request));
    }

    @PostMapping("/confirm")
    public DataResponse<AdminDaeguChainTokenDto.RewardTransfer> confirm(
            @PathVariable Long transactionId, @Valid @RequestBody AdminRewardTransferRecoveryDto.Request request) {
        return new DataResponse<>(service.confirm(transactionId, request));
    }
}
