package com.kaii.dentix.domain.intakeSurvey.controller;

import com.kaii.dentix.domain.intakeSurvey.AdminIntakeSurveyService;
import com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/intake-surveys")
public class AdminIntakeSurveyController {
    private final AdminIntakeSurveyService service;

    @GetMapping(name = "슈퍼관리자 기관별 설문 현황")
    public DataResponse<AdminIntakeSurveyService.Listing> list(
            @RequestParam(required = false) String organization,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") AdminIntakeSurveyService.StatusFilter status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return new DataResponse<>(service.list(organization, keyword, status, page, size));
    }

    @GetMapping(value = "/{userId}", name = "슈퍼관리자 사용자 설문 조회")
    public DataResponse<State> get(@PathVariable Long userId) { return new DataResponse<>(service.get(userId)); }

    @PutMapping(value = "/{userId}", name = "슈퍼관리자 사용자 설문 수정")
    public DataResponse<State> update(@PathVariable Long userId, @Valid @RequestBody SaveRequest body) {
        return new DataResponse<>(service.update(userId, body));
    }
}
