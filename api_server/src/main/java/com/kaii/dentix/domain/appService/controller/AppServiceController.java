package com.kaii.dentix.domain.appService.controller;

import com.kaii.dentix.domain.appService.application.AppServiceUsageService;
import com.kaii.dentix.domain.appService.dto.AppServiceDto;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.user.dto.UserServiceUsageDto;
import com.kaii.dentix.global.common.response.DataResponse;
import com.kaii.dentix.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-service")
public class AppServiceController {

    private final AppServiceUsageService appServiceUsageService;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * 앱 서비스 목록 및 연동 현황 조회
     */
    @GetMapping("/list")
    public DataResponse<List<AppServiceDto.UsageStatus>> getAppServiceList(
            HttpServletRequest request
    ) {
        Long userId = jwtTokenUtil.getUserId(request.getHeader("Authorization"), TokenType.AccessToken);
        return new DataResponse<>(appServiceUsageService.getAppServiceUsageList(userId));
    }

    /**
     * 앱 서비스 연동하기
     */
    @PostMapping("/connect")
    public SuccessResponse connectAppService(
            HttpServletRequest request,
            @RequestBody @Valid AppServiceDto.ConnectRequest connectRequest
    ) {
        Long userId = jwtTokenUtil.getUserId(request.getHeader("Authorization"), TokenType.AccessToken);
        appServiceUsageService.connectAppService(userId, connectRequest);
        return new SuccessResponse();
    }
}