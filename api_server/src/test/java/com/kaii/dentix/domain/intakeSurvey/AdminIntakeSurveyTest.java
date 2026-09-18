package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.databind.*;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;

class AdminIntakeSurveyTest {
    final ObjectMapper mapper = new ObjectMapper();
    final AdminIntakeSurveyRepository users = mock(AdminIntakeSurveyRepository.class);
    final IntakeSurveyRepository surveys = mock(IntakeSurveyRepository.class);
    IntakeSurveyTemplate template;
    AdminIntakeSurveyService service;
    @BeforeEach void setup() throws Exception {
        template = new IntakeSurveyTemplate(mapper);
        service = new AdminIntakeSurveyService(users, surveys,
                new IntakeSurveyService(mock(UserService.class), surveys, template, mapper), template, mapper);
        when(surveys.lockUser(42L)).thenReturn(User.builder().userId(42L).build());
        when(surveys.saveAndFlush(any())).thenAnswer(i -> {
            UserIntakeSurvey survey = i.getArgument(0);
            ReflectionTestUtils.setField(survey, "revision", survey.getRevision() == null ? 0L : survey.getRevision() + 1);
            return survey;
        });
    }
    Map<String, JsonNode> answers() {
        Map<String, JsonNode> answers = new HashMap<>();
        template.get().sections().forEach(s -> s.questions().stream().filter(q -> q.type().equals("single"))
            .forEach(q -> answers.put(q.key(), mapper.valueToTree(q.options().get(0).value()))));
        return answers;
    }
    SaveRequest request(Map<String, JsonNode> answers, Long revision) {
        return new SaveRequest(template.get().version(), answers, 1, revision);
    }
    @Test void newDraftStaysRequiredAndDoesNotCreateOtherUsers() {
        var state = service.update(42L, request(Map.of("eat10_1", mapper.valueToTree(2)), null));
        assertThat(state.completed()).isFalse();
        assertThat(state.surveyScores()).isEmpty();
        assertThat(state.revision()).isZero();
        assertThatThrownBy(() -> service.update(99L, request(Map.of(), null))).isInstanceOf(FormValidationException.class);
    }
    @Test void completedEditPreservesOriginalSubmissionTimeAndRecalculatesScore() throws Exception {
        var survey = new UserIntakeSurvey(42L);
        survey.save(template.get().version(), mapper.writeValueAsString(answers()), "{}", 7, true);
        ReflectionTestUtils.setField(survey, "revision", 3L);
        var completedAt = survey.getCompletedAt();
        when(surveys.findById(42L)).thenReturn(Optional.of(survey));
        var changed = answers(); changed.put("eat10_1", mapper.valueToTree(4));
        var result = service.update(42L, request(changed, 3L));
        assertThat(result.completedAt()).isEqualTo(completedAt);
        assertThat(result.surveyScores()).containsEntry("1", 4);
        assertThat(result.revision()).isEqualTo(4L);
    }
    @Test void incompleteCompletedSurveyAndStaleUpdatesAreRejected() {
        var survey = new UserIntakeSurvey(42L);
        survey.save(template.get().version(), "{}", "{}", 7, true);
        ReflectionTestUtils.setField(survey, "revision", 3L);
        when(surveys.findById(42L)).thenReturn(Optional.of(survey));
        assertThatThrownBy(() -> service.update(42L, request(Map.of(), 3L))).isInstanceOf(FormValidationException.class);
        assertThatThrownBy(() -> service.update(42L, request(answers(), 2L))).isInstanceOf(FormValidationException.class);
        verify(surveys, never()).saveAndFlush(any());
    }
    @Test void missingUserCannotBeReadAndInvalidPagingIsRejected() {
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(FormValidationException.class);
        assertThatThrownBy(() -> service.list(null, "", AdminIntakeSurveyService.StatusFilter.ALL, 0, 101))
            .isInstanceOf(FormValidationException.class);
    }
}
