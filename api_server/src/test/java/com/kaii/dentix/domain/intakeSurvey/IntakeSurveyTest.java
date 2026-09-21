package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.databind.*;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntakeSurveyTest {
    final ObjectMapper mapper = new ObjectMapper();
    IntakeSurveyTemplate template;
    final UserService users = mock(UserService.class);
    final IntakeSurveyRepository repository = mock(IntakeSurveyRepository.class);
    final HttpServletRequest http = mock(HttpServletRequest.class);
    IntakeSurveyService service;

    @BeforeEach void setup() throws Exception {
        template = new IntakeSurveyTemplate(mapper);
        service = new IntakeSurveyService(users, repository, template, mapper);
        when(users.getTokenUser(http)).thenReturn(User.builder().userId(42L).build());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            UserIntakeSurvey survey = invocation.getArgument(0);
            ReflectionTestUtils.setField(survey, "revision", survey.getRevision() == null ? 0L : survey.getRevision() + 1);
            when(repository.findById(42L)).thenReturn(Optional.of(survey));
            return survey;
        });
    }

    Map<String, JsonNode> fullAnswers() {
        Map<String, JsonNode> answers = new LinkedHashMap<>();
        template.get().sections().forEach(section -> section.questions().stream()
                .filter(question -> question.type().equals("single"))
                .forEach(question -> answers.put(question.key(), mapper.valueToTree(question.options().get(0).value()))));
        return answers;
    }
    SaveRequest body(Map<String, JsonNode> answers, Long revision) {
        return new SaveRequest(template.get().version(), answers, 7, revision);
    }

    @Test void sourceHasSevenSectionsAndAll47Questions() {
        assertThat(template.get().sections()).hasSize(7);
        assertThat(template.get().sections().stream().map(section -> section.questions().size()).toList())
                .containsExactly(10, 12, 5, 4, 5, 6, 5);
    }
    @Test void newAndExistingUsersWithoutSubmissionAreRequired() {
        assertThat(service.status(http).required()).isTrue();
        assertThat(service.get(http).currentTab()).isEqualTo(1);
    }
    @Test void draftSurvivesReloadAndDoesNotCompleteSurvey() {
        var saved = service.save(http, body(Map.of("eat10_1", mapper.valueToTree(0)), null), false);
        assertThat(saved.revision()).isEqualTo(0L);
        assertThat(service.get(http).surveyAnswers()).containsKey("eat10_1");
        assertThat(service.status(http).required()).isTrue();
        verify(repository).lockUser(42L);
    }
    @Test void incompleteSubmissionCannotUnlockUserPages() {
        assertThatThrownBy(() -> service.save(http, body(Map.of(), null), true)).isInstanceOf(FormValidationException.class);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void zeroAnswersAreValidAndNoDentalVisitSkipsFrequency() {
        var result = service.save(http, body(fullAnswers(), null), true);
        assertThat(result.completed()).isTrue();
        assertThat(result.surveyAnswers()).doesNotContainKey("dental_2");
        assertThat(result.surveyScores()).containsEntry("1", 0).containsEntry("6", 0);
        assertThat(service.status(http).required()).isFalse();
    }
    @Test void dentalFrequencyRequiredOnlyWhenVisited() {
        var answers = fullAnswers();
        answers.put("dental_1", mapper.valueToTree(1));
        answers.remove("dental_2");
        assertThatThrownBy(() -> template.validate(body(answers, null), true)).isInstanceOf(FormValidationException.class);
        answers.put("dental_2", mapper.valueToTree(2));
        assertThat(template.validate(body(answers, null), true)).containsKey("dental_2");
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "5", "0.5", "\"0\"", "true", "4294967296"})
    void refusesOutOfRangeAndNonIntegerOptions(String value) throws Exception {
        var answers = fullAnswers(); answers.put("eat10_1", mapper.readTree(value));
        assertThatThrownBy(() -> template.validate(body(answers, null), false)).isInstanceOf(FormValidationException.class);
    }
    @Test void mnaStressHasOnlyZeroOrTwoPoints() {
        var answers = fullAnswers(); answers.put("mna_d", mapper.valueToTree(1));
        assertThatThrownBy(() -> template.validate(body(answers, null), true)).isInstanceOf(FormValidationException.class);
    }
    @Test void calculatesMaximumSourceScoresWithoutCombiningIncompatibleScales() {
        var answers = fullAnswers();
        template.get().sections().forEach(section -> section.questions().stream().filter(q -> q.type().equals("single"))
                .forEach(q -> answers.put(q.key(), mapper.valueToTree(q.options().get(q.options().size() - 1).value()))));
        assertThat(template.scores(template.validate(body(answers, null), true)))
                .containsExactlyInAnyOrderEntriesOf(Map.of("1",40,"2",12,"3",5,"4",4,"5",10,"6",14));
    }
    @ParameterizedTest @ValueSource(strings = {"\"2026-13\"", "\"2999-01\"", "\"1899-01\"", "[]"})
    void refusesInvalidDentalMonth(String value) throws Exception {
        var answers = fullAnswers(); answers.put("dental_3", mapper.readTree(value));
        assertThatThrownBy(() -> template.validate(body(answers, null), true)).isInstanceOf(FormValidationException.class);
    }
    @ParameterizedTest @ValueSource(strings = {"[0,0]", "[5]", "[\"1\"]", "1"})
    void refusesInvalidDentalMultipleChoice(String value) throws Exception {
        var answers = fullAnswers(); answers.put("dental_5", mapper.readTree(value));
        assertThatThrownBy(() -> template.validate(body(answers, null), true)).isInstanceOf(FormValidationException.class);
    }
    @Test void emptyMultipleChoiceAndUnknownDateAreAllowed() throws Exception {
        var answers = fullAnswers(); answers.put("dental_5", mapper.readTree("[]"));
        answers.put("dental_3", mapper.valueToTree(""));
        assertThat(template.validate(body(answers, null), true)).doesNotContainKey("dental_3");
    }
    @Test void refusesUnknownQuestionsAndOldVersion() {
        var answers = fullAnswers(); answers.put("userId", mapper.valueToTree(99));
        assertThatThrownBy(() -> template.validate(body(answers, null), true)).isInstanceOf(FormValidationException.class);
        assertThatThrownBy(() -> template.validate(new SaveRequest("old", Map.of(),1,null), false)).isInstanceOf(FormValidationException.class);
    }
    @Test void eatDisplayChangesWithoutChangingStoredScores() {
        var options = template.get().sections().get(0).questions().get(0).options();
        assertThat(options.stream().map(Option::value)).containsExactly(0, 1, 2, 3, 4);
        assertThat(options.stream().map(Option::label)).containsExactly("1 · 문제없음", "2", "3 · 보통", "4", "5 · 심각함");
    }
    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void acceptsDentalVisitPeriods(int value) {
        var answers = fullAnswers(); answers.put("dental_3", mapper.valueToTree(value));
        assertThat(template.validate(body(answers, null), true)).containsEntry("dental_3", mapper.valueToTree(value));
    }
    @ParameterizedTest @ValueSource(strings = {"0", "7", "1.5", "true", "\"1\""})
    void refusesInvalidDentalVisitPeriods(String value) throws Exception {
        var answers = fullAnswers(); answers.put("dental_3", mapper.readTree(value));
        assertThatThrownBy(() -> template.validate(body(answers, null), true)).isInstanceOf(FormValidationException.class);
    }
    @Test void oldDraftAndOpenV1ClientRetainVisitMonthAndScores() {
        var answers = fullAnswers(); answers.put("dental_3", mapper.valueToTree("2026-08"));
        var saved = service.save(http, new SaveRequest("2026-09-17-v1", answers, 7, null), false);
        assertThat(saved.surveyAnswers()).containsEntry("dental_3", mapper.valueToTree("2026-08"));
        var completed = service.save(http, body(saved.surveyAnswers(), saved.revision()), true);
        assertThat(completed.surveyAnswers()).containsEntry("dental_3", mapper.valueToTree("2026-08"));
        assertThat(completed.surveyScores()).containsEntry("1", 0);
    }
    @Test void duplicateSubmissionAndLateDraftCannotOverwriteCompletion() {
        var first = service.save(http, body(fullAnswers(), null), true);
        assertThat(service.save(http, body(Map.of(), null), true).completedAt()).isEqualTo(first.completedAt());
        assertThat(service.save(http, body(Map.of(), null), false).surveyAnswers()).isEqualTo(first.surveyAnswers());
        verify(repository, times(1)).saveAndFlush(any());
    }
    @Test void staleDraftCannotOverwriteAnotherTab() {
        service.save(http, body(Map.of(), null), false);
        assertThatThrownBy(() -> service.save(http, body(Map.of(), null), false)).isInstanceOf(FormValidationException.class);
        verify(repository, times(1)).saveAndFlush(any());
    }
    @Test void stateIsIsolatedByAuthenticatedUser() {
        service.save(http, body(fullAnswers(), null), true);
        when(users.getTokenUser(http)).thenReturn(User.builder().userId(99L).build());
        assertThat(service.status(http).required()).isTrue();
        assertThat(service.get(http).surveyAnswers()).isEmpty();
    }
    @Test void unauthenticatedOrAdminCannotReadOrWriteSurvey() {
        when(users.getTokenUser(http)).thenThrow(new UnauthorizedException("User permission is required."));
        assertThatThrownBy(() -> service.get(http)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.save(http, body(fullAnswers(), null), true)).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(repository);
    }
}
