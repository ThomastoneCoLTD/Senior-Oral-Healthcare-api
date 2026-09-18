package com.kaii.dentix.domain.oralExercise;

import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService;
import com.kaii.dentix.domain.oralExercise.dao.*;
import com.kaii.dentix.domain.oralExercise.domain.*;
import com.kaii.dentix.domain.reward.dao.UserRewardTransactionRepository;
import com.kaii.dentix.domain.reward.domain.*;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class OralExerciseHistoryServiceTest {
    final OralExerciseInteractionLogRepository logs = mock(OralExerciseInteractionLogRepository.class);
    final OralExerciseContentRepository contents = mock(OralExerciseContentRepository.class);
    final UserOralExerciseProgressRepository progress = mock(UserOralExerciseProgressRepository.class);
    final UserRewardTransactionRepository rewards = mock(UserRewardTransactionRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final OralExerciseHistoryService service = new OralExerciseHistoryService(logs, contents, progress, rewards, users, mock(JwtTokenUtil.class));
    final OralExerciseContent content = OralExerciseContent.builder().oralExerciseContentId(7L).contentSort(2).title("Chapter 1").active(true).build();
    final OralExerciseInteractionLogRepository.Counts counts = mock(OralExerciseInteractionLogRepository.Counts.class);
    @BeforeEach void setup() {
        when(contents.findAll()).thenReturn(List.of(content));
        when(counts.getContentId()).thenReturn(7L);
        when(logs.summarize(1L)).thenReturn(List.of(counts));
        when(rewards.findByUserIdOrderByCreatedDesc(1L)).thenReturn(List.of());
        when(progress.findByUserId(1L)).thenReturn(List.of());
    }
    @Test void thresholdIsPerVideoAndSuccessRemovesReminderWithoutErasingFailures() {
        when(counts.getFailures()).thenReturn(2L);
        assertThat(service.summary(1L).get(0).retryRequired()).isFalse();
        when(counts.getFailures()).thenReturn(3L);
        assertThat(service.summary(1L).get(0).retryRequired()).isTrue();
        when(rewards.findByUserIdOrderByCreatedDesc(1L)).thenReturn(List.of(UserRewardTransaction.builder()
                .coinId("essential_video_1").type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFERRED).build()));
        var result = service.summary(1L).get(0);
        assertThat(result.retryRequired()).isFalse();
        assertThat(result.rewardReceived()).isTrue();
        assertThat(result.failureCount()).isEqualTo(3);
    }
    @Test void failedTransferIsNotReceivedAndLegacyProgressStillShowsUnrewardedVideo() {
        when(counts.getFailures()).thenReturn(3L);
        when(rewards.findByUserIdOrderByCreatedDesc(1L)).thenReturn(List.of(UserRewardTransaction.builder()
                .coinId("essential_video_1").type(UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .status(UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED).build()));
        when(progress.findByUserId(1L)).thenReturn(List.of(UserOralExerciseProgress.builder().content(content).completed(true).viewCount(300).build()));
        var result = service.summary(1L).get(0);
        assertThat(result.retryRequired()).isTrue();
        assertThat(result.watched()).isTrue();
        assertThat(result.completedViews()).isZero(); // Never use request count as watch count.
    }
    @Test void inactiveAndCompletedRewardJourneysDoNotRequestReplay() {
        when(counts.getFailures()).thenReturn(3L);
        when(contents.findAll()).thenReturn(List.of(OralExerciseContent.builder().oralExerciseContentId(7L).contentSort(2).active(false).build()));
        assertThat(service.summary(1L).get(0).retryRequired()).isFalse();
        when(contents.findAll()).thenReturn(List.of(content));
        List<UserRewardTransaction> transactions = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            transactions.add(UserRewardTransaction.builder().coinId("essential_video_" + n).type(UserRewardTransactionType.ORAL_EXERCISE_COIN).status(UserRewardTransactionStatus.TOKEN_TRANSFERRED).build());
            transactions.add(UserRewardTransaction.builder().coinId("essential_video_" + n).type(UserRewardTransactionType.ORAL_EXERCISE_RECLAIM).status(UserRewardTransactionStatus.TOKEN_TRANSFERRED).build());
        }
        when(rewards.findByUserIdOrderByCreatedDesc(1L)).thenReturn(transactions);
        assertThat(service.summary(1L).get(0).retryRequired()).isFalse();
    }
    @Test void failureRequiresAcceptedPlaybackAndCannotBeForgedAsServerError() {
        var wrong = new OralExerciseHistoryService.FailureRequest(7L, "s1", OralExerciseInteractionEventType.TOKEN_WRONG);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> service.recordFailure(1L, wrong, false)).hasMessageContaining("시작된");
        assertThatThrownBy(() -> service.recordFailure(1L, new OralExerciseHistoryService.FailureRequest(7L,"s1", OralExerciseInteractionEventType.TOKEN_FAILED), false)).hasMessageContaining("올바르지");
        verify(logs, never()).save(any());
    }
    @Test void failureIsSavedOnlyOncePerSessionUnderUserLock() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(new User()));
        when(contents.findById(7L)).thenReturn(Optional.of(content));
        when(logs.existsByUserIdAndContent_OralExerciseContentIdAndSessionIdAndEventTypeIn(eq(1L),eq(7L),eq("s1"),anyList())).thenAnswer(call ->
                !call.getArgument(3).equals(OralExerciseHistoryService.FAILURES));
        var body = new OralExerciseHistoryService.FailureRequest(7L,"s1", OralExerciseInteractionEventType.TOKEN_TIMEOUT);
        service.recordFailure(1L,body,false);
        var order = inOrder(users, logs);
        order.verify(users).findByIdForUpdate(1L);
        order.verify(logs).existsByUserIdAndContent_OralExerciseContentIdAndSessionIdAndEventTypeIn(
                1L, 7L, "s1", List.of(OralExerciseInteractionEventType.VIEW, OralExerciseInteractionEventType.PLAY));
        verify(logs).save(argThat(l -> l.getUserId().equals(1L) && l.getEventType() == OralExerciseInteractionEventType.TOKEN_TIMEOUT));
        when(logs.existsByUserIdAndContent_OralExerciseContentIdAndSessionIdAndEventTypeIn(1L,7L,"s1",OralExerciseHistoryService.FAILURES)).thenReturn(true);
        service.recordFailure(1L,body,false);
        verify(logs, times(1)).save(any());
    }
}
