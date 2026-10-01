package com.kaii.dentix.domain.oralExercise.controller;

import com.kaii.dentix.domain.oralExercise.application.OralExerciseService;
import com.kaii.dentix.domain.oralExercise.dto.OralExerciseDto;
import com.kaii.dentix.domain.reward.application.UserRewardService;
import com.kaii.dentix.domain.reward.application.UserRewardReclaimService;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionEventType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/oral-exercise")
public class OralExerciseController {

    private final OralExerciseService oralExerciseService;
    private final UserRewardService userRewardService;
    private final UserRewardReclaimService userRewardReclaimService;
    private final OralExerciseHistoryService historyService;

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
        try {
            var reward = userRewardService.rewardOralExerciseButtonClick(request, buttonClickRequest);
            if (!buttonClickRequest.isAcceptsDeferred() &&
                    (reward.getStatus() == com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING
                    || reward.getStatus() == com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING
                    || reward.getStatus() == com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW)) {
                throw new DeferredRewardException();
            }
            return new DataResponse<>(reward);
        } catch (RuntimeException failure) {
            if (failure instanceof DeferredRewardException || failure instanceof com.kaii.dentix.domain.reward.application.DeferredRewardClientRequiredException) throw failure;
            // Reward service transaction has finished; preserve the observation even if it rolled back.
            if (buttonClickRequest.getSelectedButtonNumber() != null
                    && buttonClickRequest.getSelectedButtonNumber().equals(buttonClickRequest.getTargetButtonNumber())
                    && buttonClickRequest.getSelectedButtonNumber() >= 1
                    && buttonClickRequest.getSelectedButtonNumber() <= 5) {
                try {
                    historyService.recordFailure(historyService.currentUser(request),
                            new OralExerciseHistoryService.FailureRequest(buttonClickRequest.getContentId(),
                                    buttonClickRequest.getSessionId(), OralExerciseInteractionEventType.TOKEN_FAILED), true);
                } catch (RuntimeException historyFailure) {
                    log.warn("Could not record oral exercise reward failure, contentId={}", buttonClickRequest.getContentId());
                }
            }
            throw failure;
        }
    }

    private static class DeferredRewardException extends com.kaii.dentix.global.common.error.exception.BadRequestApiException {
        DeferredRewardException() { super("토큰 지급 결과를 확인하고 있습니다. 토큰 현황에서 확인해 주세요."); }
    }

    @PostMapping("/rewards/reclaim")
    public DataResponse<UserRewardDto.ReclaimResponse> reclaimRewards(HttpServletRequest request) {
        return new DataResponse<>(userRewardReclaimService.reclaimOralExerciseTokens(request));
    }
}
