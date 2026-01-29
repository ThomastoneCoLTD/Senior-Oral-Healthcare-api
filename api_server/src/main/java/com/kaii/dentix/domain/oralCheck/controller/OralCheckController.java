package com.kaii.dentix.domain.oralCheck.controller;

import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oralCheck")
public class OralCheckController {

    private final OralCheckService oralCheckService;

    /**
     * 구강검진 사진 촬영
     */
    // 촬영
    @PostMapping("/photo")
    public DataResponse<OralCheckDto.PhotoResponse> oralCheckPhoto(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return oralCheckService.oralCheckPhoto(request, file, "촬영");
    }

    // 업로드
    @PostMapping("/upload")
    public DataResponse<OralCheckDto.PhotoResponse> oralCheckUpload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return oralCheckService.oralCheckPhoto(request, file, "업로드");
    }


    /**
     *  구강검진 결과
     */
    @GetMapping(value = "/result", name = "구강검진 결과")
    public DataResponse<OralCheckDto.ResultResponse> oralCheckResult(
            HttpServletRequest httpServletRequest,
            @RequestParam Long oralCheckId
    ) {
        return new DataResponse<>(oralCheckService.oralCheckResult(httpServletRequest, oralCheckId));
    }

    /**
     * 대시보드 조회
     */
    @GetMapping(value = "/dashboard", name = "대시보드 조회")
    public DataResponse<OralCheckDto.DashboardResponse> dashboard(HttpServletRequest httpServletRequest) {
        return new DataResponse<>(oralCheckService.dashboard(httpServletRequest));
    }

    /**
     *  구강 상태 조회
     */
    @GetMapping(name = "구강 상태 조회")
    public DataResponse<OralCheckDto.TimelineResponse> oralCheck(HttpServletRequest httpServletRequest) {
        return new DataResponse<>(oralCheckService.oralCheck(httpServletRequest));
    }
}
