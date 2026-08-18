package com.kaii.dentix.domain.gingivitis.controller;

import com.kaii.dentix.domain.gingivitis.dto.GingivitisDto;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class GingivitisController {

    private final OralCheckService oralCheckService;

    @PostMapping("/gingivitis-analyses")
    public DataResponse<GingivitisDto.CreateResponse> create(
            HttpServletRequest request,
            @RequestParam("picture") MultipartFile picture
    ) throws Exception {
        return new DataResponse<>(oralCheckService.createGingivitisAnalysis(request, picture));
    }

    @GetMapping("/gingivitis-analyses/{analysisId}")
    public DataResponse<GingivitisDto.DetailResponse> detail(
            HttpServletRequest request,
            @PathVariable Long analysisId
    ) {
        return new DataResponse<>(oralCheckService.gingivitisAnalysisDetail(request, analysisId));
    }

    @GetMapping("/gingivitis-condition")
    public DataResponse<GingivitisDto.ConditionResponse> condition(HttpServletRequest request) {
        return new DataResponse<>(oralCheckService.gingivitisCondition(request));
    }
}
