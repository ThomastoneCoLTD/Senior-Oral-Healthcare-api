package com.kaii.dentix.domain.oralExercise.application;

import com.kaii.dentix.domain.jwt.*;
import com.kaii.dentix.domain.oralExercise.dao.*;
import com.kaii.dentix.domain.oralExercise.domain.*;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.domain.*;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OralExerciseHistoryService {
    public static final List<OralExerciseInteractionEventType> FAILURES = List.of(
            OralExerciseInteractionEventType.TOKEN_WRONG, OralExerciseInteractionEventType.TOKEN_TIMEOUT,
            OralExerciseInteractionEventType.TOKEN_FAILED);
    private static final List<OralExerciseInteractionEventType> HISTORY = List.of(
            OralExerciseInteractionEventType.VIEW, OralExerciseInteractionEventType.COMPLETE,
            OralExerciseInteractionEventType.TOKEN_WRONG, OralExerciseInteractionEventType.TOKEN_TIMEOUT,
            OralExerciseInteractionEventType.TOKEN_FAILED);
    private final OralExerciseInteractionLogRepository logs;
    private final OralExerciseContentRepository contents;
    private final UserOralExerciseProgressRepository progress;
    private final UserRewardTransactionRepository rewards;
    private final UserRepository users;
    private final JwtTokenUtil jwt;

    public record Summary(Long contentId, int sort, String title, boolean active, boolean watched,
                          long completedViews, long failureCount, long wrongCount, long timeoutCount,
                          long errorCount, boolean rewardReceived, boolean retryRequired, Date lastViewedAt) {}
    public record Member(Long userId, String name, String loginId, String organization) {}
    public record Event(Long id, String eventType, Date occurredAt, int completionRate) {}
    public record FailureRequest(Long contentId, String sessionId, OralExerciseInteractionEventType reason) {}

    public Long currentUser(HttpServletRequest request) {
        return jwt.getUserId(jwt.getAccessToken(request), TokenType.AccessToken);
    }

    @Transactional(readOnly = true)
    public List<Summary> summary(Long userId) {
        var countMap = logs.summarize(userId).stream().collect(Collectors.toMap(
                OralExerciseInteractionLogRepository.Counts::getContentId, c -> c));
        var progressMap = progress.findByUserId(userId).stream().collect(Collectors.toMap(
                p -> p.getContent().getOralExerciseContentId(), p -> p));
        var transactions = rewards.findByUserIdOrderByCreatedDesc(userId);
        var received = transactions.stream()
                .filter(t -> t.getType() == UserRewardTransactionType.ORAL_EXERCISE_COIN && t.isRewardReceived())
                .map(UserRewardTransaction::getCoinId).filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        boolean journeyCompleted = UserRewardJourneySummary.from(transactions).completed();
        return contents.findAll().stream().sorted(Comparator.comparingInt(OralExerciseContent::getContentSort))
                .filter(c -> countMap.containsKey(c.getOralExerciseContentId()) || progressMap.containsKey(c.getOralExerciseContentId()))
                .map(c -> {
                    var count = countMap.get(c.getOralExerciseContentId());
                    var p = progressMap.get(c.getOralExerciseContentId());
                    String token = OralExerciseRewardToken.tokenNameForContentSort(c.getContentSort());
                    boolean rewarded = token != null && received.contains(token.toLowerCase(Locale.ROOT));
                    long failures = count == null ? 0 : count.getFailures();
                    boolean watched = p != null && p.isCompleted() || count != null && count.getCompletedViews() > 0;
                    return new Summary(c.getOralExerciseContentId(), c.getContentSort(), c.getTitle(), c.isActive(), watched,
                            count == null ? 0 : count.getCompletedViews(), failures,
                            count == null ? 0 : count.getWrongCount(), count == null ? 0 : count.getTimeoutCount(),
                            count == null ? 0 : count.getErrorCount(), rewarded,
                            c.isActive() && token != null && failures >= 3 && !rewarded && !journeyCompleted,
                            count == null ? p.getLastViewedAt() : count.getLastViewedAt());
                }).toList();
    }

    @Transactional
    public void recordFailure(Long userId, FailureRequest body, boolean serverObserved) {
        if (body.contentId() == null || body.sessionId() == null || body.sessionId().isBlank()
                || body.sessionId().length() > 100 || body.reason() == null || !FAILURES.contains(body.reason())
                || !serverObserved && body.reason() == OralExerciseInteractionEventType.TOKEN_FAILED) {
            throw new BadRequestApiException("올바르지 않은 실패 기록입니다.");
        }
        users.findByIdForUpdate(userId).orElseThrow(() -> new NotFoundDataException("사용자가 없습니다."));
        // Only a previously accepted playback may create a failure; the interaction endpoint enforces locks.
        if (!logs.existsByUserIdAndContent_OralExerciseContentIdAndSessionIdAndEventTypeIn(
                userId, body.contentId(), body.sessionId(),
                List.of(OralExerciseInteractionEventType.VIEW, OralExerciseInteractionEventType.PLAY))) {
            throw new BadRequestApiException("시작된 시청 회차가 없습니다.");
        }
        if (logs.existsByUserIdAndContent_OralExerciseContentIdAndSessionIdAndEventTypeIn(
                userId, body.contentId(), body.sessionId(), FAILURES)) return;
        var content = contents.findById(body.contentId()).orElseThrow(() -> new NotFoundDataException("영상이 없습니다."));
        logs.save(OralExerciseInteractionLog.builder().userId(userId).content(content)
                .eventType(body.reason()).sessionId(body.sessionId()).build());
    }

    @Transactional(readOnly = true)
    public Page<Member> members(String keyword, int page, int size) {
        return users.findExerciseHistoryMembers(keyword.trim(), PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50))))
                .map(u -> new Member(u.getUserId(), u.getUserName(), u.getUserLoginIdentifier(), u.getRealOrganization()));
    }

    @Transactional(readOnly = true)
    public Page<Event> events(Long userId, Long contentId, int page) {
        return logs.findByUserIdAndContent_OralExerciseContentIdAndEventTypeInOrderByCreatedDescOralExerciseInteractionLogIdDesc(
                userId, contentId, HISTORY, PageRequest.of(Math.max(0, page), 20))
                .map(l -> new Event(l.getOralExerciseInteractionLogId(), l.getEventType().name(), l.getCreated(), l.getCompletionRate()));
    }
}
