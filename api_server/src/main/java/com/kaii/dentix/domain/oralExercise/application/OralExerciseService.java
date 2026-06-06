package com.kaii.dentix.domain.oralExercise.application;

import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseContentRepository;
import com.kaii.dentix.domain.oralExercise.dao.OralExerciseInteractionLogRepository;
import com.kaii.dentix.domain.oralExercise.dao.UserOralExerciseProgressRepository;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionEventType;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionLog;
import com.kaii.dentix.domain.oralExercise.domain.UserOralExerciseProgress;
import com.kaii.dentix.domain.oralExercise.dto.OralExerciseDto;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OralExerciseService {

    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final OralExerciseInteractionLogRepository oralExerciseInteractionLogRepository;
    private final UserOralExerciseProgressRepository userOralExerciseProgressRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional(readOnly = true)
    public OralExerciseDto.ListResponse getContents(HttpServletRequest request) {
        Long userId = getOptionalUserId(request);
        Map<Long, UserOralExerciseProgress> progressMap = userId == null
                ? Map.of()
                : userOralExerciseProgressRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        progress -> progress.getContent().getOralExerciseContentId(),
                        Function.identity()
                ));

        return OralExerciseDto.ListResponse.builder()
                .contents(
                        oralExerciseContentRepository.findByActiveTrueOrderByContentSortAsc()
                                .stream()
                                .map(content -> OralExerciseDto.ContentResponse.from(
                                        content,
                                        progressMap.get(content.getOralExerciseContentId())
                                ))
                                .toList()
                )
                .build();
    }

    @Transactional
    public OralExerciseDto.ProgressResponse recordInteraction(
            HttpServletRequest request,
            OralExerciseDto.InteractionRequest interactionRequest
    ) {
        Long userId = getUserId(request);

        OralExerciseContent content = oralExerciseContentRepository
                .findById(interactionRequest.getContentId())
                .orElseThrow(() -> new NotFoundDataException("존재하지 않는 구강체조 콘텐츠입니다."));

        int durationSeconds = valueOrDefault(interactionRequest.getDurationSeconds(), content.getDurationSeconds());
        int watchedSeconds = valueOrDefault(interactionRequest.getWatchedSeconds(), 0);
        int currentPositionSeconds = valueOrDefault(interactionRequest.getCurrentPositionSeconds(), 0);
        int completionRate = calculateCompletionRate(interactionRequest, currentPositionSeconds, durationSeconds);
        boolean completed = Boolean.TRUE.equals(interactionRequest.getCompleted()) || completionRate >= 95;
        OralExerciseInteractionEventType eventType = interactionRequest.getEventType() == null
                ? OralExerciseInteractionEventType.PROGRESS
                : interactionRequest.getEventType();

        oralExerciseInteractionLogRepository.save(OralExerciseInteractionLog.builder()
                .userId(userId)
                .content(content)
                .eventType(eventType)
                .watchedSeconds(watchedSeconds)
                .currentPositionSeconds(currentPositionSeconds)
                .durationSeconds(durationSeconds)
                .completionRate(completionRate)
                .completed(completed)
                .sessionId(interactionRequest.getSessionId())
                .build());

        UserOralExerciseProgress progress = userOralExerciseProgressRepository
                .findByUserIdAndContent_OralExerciseContentId(userId, content.getOralExerciseContentId())
                .orElseGet(() -> UserOralExerciseProgress.builder()
                        .userId(userId)
                        .content(content)
                        .totalWatchedSeconds(0)
                        .maxWatchedSeconds(0)
                        .lastPositionSeconds(0)
                        .completionRate(0)
                        .completed(false)
                        .viewCount(0)
                        .build());

        progress.updateProgress(
                watchedSeconds,
                currentPositionSeconds,
                durationSeconds,
                completionRate,
                completed
        );

        return OralExerciseDto.ProgressResponse.from(
                userOralExerciseProgressRepository.save(progress)
        );
    }

    private Long getUserId(HttpServletRequest request) {
        String accessToken = jwtTokenUtil.getAccessToken(request);
        if (accessToken == null) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }
        return jwtTokenUtil.getUserId(accessToken, TokenType.AccessToken);
    }

    private Long getOptionalUserId(HttpServletRequest request) {
        try {
            String accessToken = jwtTokenUtil.getAccessToken(request);
            if (accessToken == null || jwtTokenUtil.isExpired(accessToken, TokenType.AccessToken)) {
                return null;
            }
            return jwtTokenUtil.getUserId(accessToken, TokenType.AccessToken);
        } catch (Exception exception) {
            return null;
        }
    }

    private int calculateCompletionRate(
            OralExerciseDto.InteractionRequest request,
            int currentPositionSeconds,
            int durationSeconds
    ) {
        if (request.getCompletionRate() != null) {
            return clamp(request.getCompletionRate(), 0, 100);
        }
        if (durationSeconds <= 0) {
            return 0;
        }
        return clamp((int) Math.round((currentPositionSeconds * 100.0) / durationSeconds), 0, 100);
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : Math.max(value, 0);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
