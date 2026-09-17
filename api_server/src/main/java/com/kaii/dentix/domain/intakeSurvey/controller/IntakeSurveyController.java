package com.kaii.dentix.domain.intakeSurvey.controller;

import com.kaii.dentix.domain.intakeSurvey.IntakeSurveyService;
import com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/intake-survey")
public class IntakeSurveyController {
    private final IntakeSurveyService service;

    @GetMapping(value = "/status", name = "최초 설문 완료 여부")
    public DataResponse<Status> status(HttpServletRequest request) { return new DataResponse<>(service.status(request)); }

    @GetMapping(name = "최초 설문 양식과 임시저장 조회")
    public DataResponse<State> get(HttpServletRequest request) { return new DataResponse<>(service.get(request)); }

    @PutMapping(value = "/draft", name = "최초 설문 임시저장")
    public DataResponse<State> draft(HttpServletRequest request, @Valid @RequestBody SaveRequest body) {
        return new DataResponse<>(service.save(request, body, false));
    }

    @PostMapping(value = "/submit", name = "최초 설문 제출")
    public DataResponse<State> submit(HttpServletRequest request, @Valid @RequestBody SaveRequest body) {
        return new DataResponse<>(service.save(request, body, true));
    }
}
