package com.kaii.dentix.domain.appService.controller;

import com.kaii.dentix.domain.appService.application.AppServiceUsageService;
import com.kaii.dentix.domain.user.dto.UserServiceUsageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users/usage")
public class AppServiceController {
    private final AppServiceUsageService serviceUserUsageService;
    @GetMapping
    public ResponseEntity<List<UserServiceUsageDto>> getServiceUserUsage(
            @RequestParam(required = false) String serviceName
    ) {
        List<UserServiceUsageDto> result = serviceUserUsageService.getServiceUserUsage(serviceName);
        return ResponseEntity.ok(result);
    }
}
