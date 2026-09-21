package com.kaii.dentix.domain.oralExercise;
import com.kaii.dentix.domain.oralExercise.application.*;
import com.kaii.dentix.domain.oralExercise.controller.OralExerciseController;
import com.kaii.dentix.domain.reward.application.*;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class OralExerciseRewardFailureTest {
 @Test void failedIssuanceIsRecordedOutsideTheRewardTransactionAndOriginalErrorIsRetained() {
  var rewards = mock(UserRewardService.class);
  var history = mock(OralExerciseHistoryService.class);
  var controller = new OralExerciseController(mock(OralExerciseService.class),rewards,mock(UserRewardReclaimService.class),history);
  var request = mock(HttpServletRequest.class);
  var body = mock(UserRewardDto.ButtonClickRequest.class);
  when(body.getContentId()).thenReturn(10L);when(body.getSessionId()).thenReturn("s1");
  when(body.getSelectedButtonNumber()).thenReturn(2);when(body.getTargetButtonNumber()).thenReturn(2);
  when(history.currentUser(request)).thenReturn(1L);
  var failure = new BadRequestApiException("transfer failed");
  when(rewards.rewardOralExerciseButtonClick(request,body)).thenThrow(failure);
  assertThatThrownBy(()->controller.rewardButtonClick(request,body)).isSameAs(failure);
  verify(history).recordFailure(eq(1L),argThat(event->event.contentId()==10L && event.sessionId().equals("s1") && event.reason().name().equals("TOKEN_FAILED")),eq(true));
  doThrow(new RuntimeException("database unavailable")).when(history).recordFailure(any(),any(),eq(true));
  assertThatThrownBy(()->controller.rewardButtonClick(request,body)).isSameAs(failure);
 }
 @Test void receivedRewardDoesNotCreateFailureHistory() {
  var rewards = mock(UserRewardService.class);
  var history = mock(OralExerciseHistoryService.class);
  var controller = new OralExerciseController(mock(OralExerciseService.class),rewards,mock(UserRewardReclaimService.class),history);
  controller.rewardButtonClick(mock(HttpServletRequest.class),mock(UserRewardDto.ButtonClickRequest.class));
  verifyNoInteractions(history);
 }
}
