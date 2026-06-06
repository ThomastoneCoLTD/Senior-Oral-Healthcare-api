package com.kaii.dentix.domain.reward.controller;

import com.kaii.dentix.domain.reward.application.UserRewardService;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/rewards")
public class UserRewardController {

    private final UserRewardService userRewardService;

    @GetMapping("/wallet")
    public DataResponse<UserRewardDto.WalletResponse> getWallet(HttpServletRequest request) {
        return new DataResponse<>(userRewardService.getWallet(request));
    }

    @PostMapping("/wallet/connect")
    public DataResponse<UserRewardDto.WalletResponse> connectWallet(
            HttpServletRequest request,
            @RequestBody(required = false) UserRewardDto.WalletConnectRequest connectRequest
    ) {
        return new DataResponse<>(userRewardService.connectWallet(request, connectRequest));
    }

    @GetMapping("/transactions")
    public DataResponse<UserRewardDto.TransactionListResponse> getTransactions(HttpServletRequest request) {
        return new DataResponse<>(userRewardService.getTransactions(request));
    }
}
