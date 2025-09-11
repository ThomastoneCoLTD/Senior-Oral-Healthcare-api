package com.kaii.dentix.domain.oralCheck.controller;

import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dto.DashboardDto;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckPhotoDto;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckResultDto;
import com.kaii.dentix.domain.organization.application.OrganizationUsageService;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import com.kaii.dentix.global.common.response.DataResponse;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oralCheck")
public class OralCheckController {

    private final OralCheckService oralCheckService;
    private final OrganizationUsageService organizationUsageService;

    /**
     * 구강검진 사진 촬영
     */
    // 촬영
    @PostMapping("/photo")
    public DataResponse<OralCheckPhotoDto> oralCheckPhoto(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return oralCheckService.oralCheckPhoto(request, file, "촬영");
    }

    // 업로드
    @PostMapping("/upload")
    public DataResponse<OralCheckPhotoDto> oralCheckUpload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return oralCheckService.oralCheckPhoto(request, file, "업로드");
    }


    /**
     *  구강검진 결과
     */
    @GetMapping(value = "/result", name = "구강검진 결과")
    public DataResponse<OralCheckResultDto> oralCheckResult(HttpServletRequest httpServletRequest, @RequestParam Long oralCheckId){
        DataResponse<OralCheckResultDto> response = new DataResponse<>(oralCheckService.oralCheckResult(httpServletRequest, oralCheckId));
        return response;
    }

    /**
     * 대시보드 조회
     */
    @GetMapping(value = "/dashboard", name = "대시보드 조회")
    public DataResponse<DashboardDto> dashboard(HttpServletRequest httpServletRequest){
        DataResponse<DashboardDto> response = new DataResponse<>(oralCheckService.dashboard(httpServletRequest));
        return response;
    }

    /**
     *  구강 상태 조회
     */
    @GetMapping(name = "구강 상태 조회")
    public DataResponse<OralCheckDto> oralCheck(HttpServletRequest httpServletRequest){
        DataResponse<OralCheckDto> response = new DataResponse<>(oralCheckService.oralCheck(httpServletRequest));
        return response;
    }
}
