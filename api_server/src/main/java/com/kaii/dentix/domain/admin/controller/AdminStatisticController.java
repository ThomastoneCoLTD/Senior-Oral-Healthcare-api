package com.kaii.dentix.domain.admin.controller;

import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/statistic")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminStatisticController {

    private final AdminStatisticService adminStatisticService;

    @GetMapping("/org/users")
    public ResponseEntity<DataResponse<AdminStatisticDto.OrgUserStatsResponse>> getOrganizationUserStatistics(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                new DataResponse<>(adminStatisticService.getOrganizationUserStatistics(httpRequest))
        );
    }
}
