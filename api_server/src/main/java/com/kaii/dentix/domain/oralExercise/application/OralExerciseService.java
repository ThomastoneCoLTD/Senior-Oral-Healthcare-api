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
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.domain.OralExerciseRewardToken;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OralExerciseService {

    private final OralExerciseContentRepository oralExerciseContentRepository;
    private final OralExerciseInteractionLogRepository oralExerciseInteractionLogRepository;
    private final UserOralExerciseProgressRepository userOralExerciseProgressRepository;
    private final UserRewardTransactionRepository userRewardTransactionRepository;
    private final UserRepository userRepository;
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
        Set<String> rewardedTokenNames = userId == null
                ? Set.of()
                : userRewardTransactionRepository
                .findByUserIdOrderByCreatedDesc(userId)
                .stream()
                .filter(transaction -> transaction.getCoinId() != null)
                .filter(transaction -> transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .filter(transaction -> transaction.getStatus() != UserRewardTransactionStatus.CANCELED)
                .map(transaction -> transaction.getCoinId().toLowerCase())
                .collect(Collectors.toSet());
        int currentWeek = calculateCurrentWeek(userId);
        List<OralExerciseDto.ContentResponse> contents = oralExerciseContentRepository.findByActiveTrueOrderByContentSortAsc()
                .stream()
                .map(content -> OralExerciseDto.ContentResponse.from(
                        content,
                        progressMap.get(content.getOralExerciseContentId()),
                        currentWeek,
                        rewardedTokenNames.contains(resolveRewardTokenName(content))
                ))
                .toList();

        return OralExerciseDto.ListResponse.builder()
                .currentWeek(Math.max(currentWeek, 1))
                .currentContent(contents.stream()
                        .filter(OralExerciseDto.ContentResponse::isCurrentWeekContent)
                        .findFirst()
                        .orElse(null))
                .previousContents(contents.stream()
                        .filter(content -> currentWeek > 0 && content.getWeek() < currentWeek)
                        .toList())
                .extraContents(contents.stream()
                        .filter(content -> content.getWeek() > 5)
                        .toList())
                .contents(contents)
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

    private int calculateCurrentWeek(Long userId) {
        if (userId == null) {
            return 0;
        }
        return userRepository.findById(userId)
                .map(User::getCreated)
                .map(created -> {
                    LocalDate startDate = created.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    long days = ChronoUnit.DAYS.between(startDate, LocalDate.now(ZoneId.systemDefault()));
                    return (int) (Math.max(days, 0) / 7) + 1;
                })
                .orElse(1);
    }

    private String resolveRewardTokenName(OralExerciseContent content) {
        String tokenName = OralExerciseRewardToken.tokenNameForContentSort(content.getContentSort());
        return tokenName == null ? null : tokenName.toLowerCase();
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
