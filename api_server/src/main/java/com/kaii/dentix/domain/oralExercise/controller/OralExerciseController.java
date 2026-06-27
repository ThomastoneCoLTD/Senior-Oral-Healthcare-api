package com.kaii.dentix.domain.oralExercise.controller;

import com.kaii.dentix.domain.oralExercise.application.OralExerciseService;
import com.kaii.dentix.domain.oralExercise.dto.OralExerciseDto;
import com.kaii.dentix.domain.reward.application.UserRewardService;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oral-exercise")
public class OralExerciseController {

    private final OralExerciseService oralExerciseService;
    private final UserRewardService userRewardService;

    @GetMapping
    public DataResponse<OralExerciseDto.ListResponse> getContents(HttpServletRequest request) {
        return new DataResponse<>(oralExerciseService.getContents(request));
    }

    @PostMapping("/interactions")
    public DataResponse<OralExerciseDto.ProgressResponse> recordInteraction(
            HttpServletRequest request,
            @RequestBody OralExerciseDto.InteractionRequest interactionRequest
    ) {
        return new DataResponse<>(oralExerciseService.recordInteraction(request, interactionRequest));
    }

    @PostMapping("/rewards/button-click")
    public DataResponse<UserRewardDto.RewardResponse> rewardButtonClick(
            HttpServletRequest request,
            @RequestBody UserRewardDto.ButtonClickRequest buttonClickRequest
    ) {
        return new DataResponse<>(userRewardService.rewardOralExerciseButtonClick(request, buttonClickRequest));
    }
}
