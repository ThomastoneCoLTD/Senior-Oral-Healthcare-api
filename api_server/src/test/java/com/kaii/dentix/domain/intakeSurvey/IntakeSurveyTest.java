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

    @Test void sourceHasElevenSectionsAndAll72Questions() {
        assertThat(template.get().sections()).hasSize(11);
        assertThat(template.get().sections().stream().map(section -> section.questions().size()).toList())
                .containsExactly(10, 12, 5, 4, 5, 6, 5, 6, 6, 6, 7);
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
    @Test void draftPersistsIndependentPartialTotalsAndRecalculatesChangedOrClearedAnswers() {
        var answers = new LinkedHashMap<String, JsonNode>();
        answers.put("eat10_1", mapper.valueToTree(4));
        answers.put("eat10_2", mapper.valueToTree(2));
        answers.put("edsq_1", mapper.valueToTree(1));
        answers.put("mna_f", mapper.valueToTree(3));
        var draft = service.save(http, body(answers, null), false);
        assertThat(draft.surveyScores()).containsExactlyInAnyOrderEntriesOf(
                Map.of("1", 6, "2", 1, "3", 0, "4", 0, "5", 0, "6", 3, "8", 0, "9", 0, "10", 0, "11", 0));
        assertThat(service.get(http).surveyScores()).isEqualTo(draft.surveyScores());
        assertThat(draft.completed()).isFalse();
        assertThat(draft.completedAt()).isNull();
        answers.put("eat10_1", mapper.valueToTree(0));
        answers.remove("eat10_2");
        var updated = service.save(http, body(answers, draft.revision()), false);
        assertThat(updated.surveyScores()).containsEntry("1", 0).containsEntry("2", 1).containsEntry("6", 3);
        var cleared = service.save(http, body(Map.of(), updated.revision()), false);
        assertThat(cleared.surveyScores()).hasSize(10).allSatisfy((section, score) -> assertThat(score).isZero());
        assertThat(service.status(http).required()).isTrue();
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
                .containsExactlyInAnyOrderEntriesOf(Map.of("1",40,"2",12,"3",5,"4",4,"5",10,"6",14,"8",30,"9",30,"10",30,"11",35));
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
    @Test void supplementalSourceSectionsKeepOneToFiveScoresAndIndependentMaximums() {
        var added = template.get().sections().subList(7, 11);
        assertThat(added.stream().map(Section::scoreMax)).containsExactly(30, 30, 30, 35);
        assertThat(added.get(0).questions().get(0).text()).isEqualTo("음식을 삼키기 위해 여러 번 삼켜야 할 때가 있다.");
        assertThat(added.get(3).questions().get(6).text()).isEqualTo("구강건강 교육이나 정보를 활용하는 것이 구강건강관리에 도움이 된다고 생각한다.");
        added.forEach(section -> section.questions().forEach(question -> {
            assertThat(question.required()).isTrue();
            assertThat(question.options().stream().map(Option::value)).containsExactly(1, 2, 3, 4, 5);
        }));
        assertThat(template.scores(fullAnswers())).containsEntry("8", 6).containsEntry("9", 6)
                .containsEntry("10", 6).containsEntry("11", 7).doesNotContainKey("7");
    }
    @ParameterizedTest @ValueSource(strings = {"0", "6", "1.5", "true", "\"1\""})
    void supplementalScoresRejectValuesOutsideTheSourceScale(String value) throws Exception {
        var answers = fullAnswers(); answers.put("swallow_symptoms_1", mapper.readTree(value));
        assertThatThrownBy(() -> template.validate(body(answers, null), false)).isInstanceOf(FormValidationException.class);
    }
    @Test void supplementalPartialTotalsSurviveReloadAndClearing() {
        Map<String, JsonNode> answers = Map.of("swallow_symptoms_1", mapper.valueToTree(1), "swallow_symptoms_6", mapper.valueToTree(5),
                "repeated_swallow_2", mapper.valueToTree(3), "tongue_function_4", mapper.valueToTree(2), "oral_awareness_7", mapper.valueToTree(4));
        var draft = service.save(http, new SaveRequest(template.get().version(), answers, 11, null), false);
        assertThat(draft.surveyScores()).containsEntry("8", 6).containsEntry("9", 3).containsEntry("10", 2).containsEntry("11", 4);
        assertThat(service.get(http).currentTab()).isEqualTo(11);
        assertThat(service.get(http).surveyScores()).isEqualTo(draft.surveyScores());
        var cleared = service.save(http, new SaveRequest(template.get().version(), Map.of(), 11, draft.revision()), false);
        assertThat(cleared.surveyScores()).containsEntry("8", 0).containsEntry("9", 0).containsEntry("10", 0).containsEntry("11", 0);
    }
    @ParameterizedTest @ValueSource(strings = {"2026-09-17-v1", "2026-09-19-v2"})
    void legacyClientsMaySaveDraftsButMustReloadBeforeSubmission(String version) {
        var legacy = fullAnswers();
        template.get().sections().subList(7, 11).forEach(section -> section.questions().forEach(q -> legacy.remove(q.key())));
        var draft = service.save(http, new SaveRequest(version, legacy, 7, null), false);
        assertThat(draft.surveyAnswers()).containsKey("eat10_1").doesNotContainKey("swallow_symptoms_1");
        assertThat(draft.template().version()).isEqualTo("2026-09-30-v3");
        assertThatThrownBy(() -> service.save(http, new SaveRequest(version, legacy, 7, draft.revision()), true))
                .isInstanceOf(FormValidationException.class).hasMessageContaining("새 문항");
        assertThatThrownBy(() -> service.save(http, body(legacy, draft.revision()), true)).isInstanceOf(FormValidationException.class);
        assertThat(service.status(http).required()).isTrue();
    }
    @Test void legacyCompletionRemainsCompleteUntilNewAnswersAreExplicitlySaved() throws Exception {
        var legacy = fullAnswers();
        template.get().sections().subList(7, 11).forEach(section -> section.questions().forEach(q -> legacy.remove(q.key())));
        var survey = new UserIntakeSurvey(42L);
        survey.save("2026-09-19-v2", mapper.writeValueAsString(legacy), "{\"1\":0}", 7, true);
        ReflectionTestUtils.setField(survey, "revision", 3L);
        when(repository.findById(42L)).thenReturn(Optional.of(survey));
        var original = service.get(http);
        assertThat(original.completed()).isTrue();
        assertThat(service.status(http).required()).isFalse();
        assertThat(original.surveyAnswers()).isEqualTo(legacy);
        assertThat(original.surveyScores()).containsOnlyKeys("1");
        assertThatThrownBy(() -> service.updateCompleted(http, body(legacy, 3L))).isInstanceOf(FormValidationException.class);
        var updated = service.updateCompleted(http, body(fullAnswers(), 3L));
        assertThat(updated.completedAt()).isEqualTo(original.completedAt());
        assertThat(updated.surveyScores()).containsEntry("8", 6).containsEntry("11", 7);
        assertThat(updated.surveyAnswers()).containsEntry("swallow_symptoms_1", mapper.valueToTree(1));
    }
    @Test void requestLimitsAllow72AnswersAndElevenSectionsButRejectLargerPayloads() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var answers = fullAnswers(); answers.put("dental_5", mapper.createArrayNode());
            assertThat(answers).hasSize(72);
            assertThat(validator.validate(new SaveRequest(template.get().version(), answers, 11, null))).isEmpty();
            assertThat(validator.validate(new SaveRequest(template.get().version(), answers, 12, null))).isNotEmpty();
            answers.put("extra", mapper.valueToTree(1));
            assertThat(validator.validate(new SaveRequest(template.get().version(), answers, 11, null))).isNotEmpty();
        }
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
    @Test void completedSurveyCanBeEditedWithoutChangingFirstSubmissionTime() {
        var first = service.save(http, body(fullAnswers(), null), true);
        var answers = fullAnswers();
        answers.put("eat10_1", mapper.valueToTree(4));
        var updated = service.updateCompleted(http, body(answers, first.revision()));
        assertThat(updated.completed()).isTrue();
        assertThat(updated.completedAt()).isEqualTo(first.completedAt());
        assertThat(updated.revision()).isEqualTo(first.revision() + 1);
        assertThat(updated.surveyAnswers()).containsEntry("eat10_1", mapper.valueToTree(4));
        assertThat(updated.surveyScores()).containsEntry("1", 4);
        assertThat(service.status(http).required()).isFalse();
        verify(repository, times(2)).lockUser(42L);
    }
    @Test void completedEditRejectsMissingAnswersAndStaleRevision() {
        var first = service.save(http, body(fullAnswers(), null), true);
        assertThatThrownBy(() -> service.updateCompleted(http, body(Map.of(), first.revision())))
                .isInstanceOf(FormValidationException.class);
        service.updateCompleted(http, body(fullAnswers(), first.revision()));
        assertThatThrownBy(() -> service.updateCompleted(http, body(fullAnswers(), first.revision())))
                .isInstanceOf(FormValidationException.class);
        verify(repository, times(2)).saveAndFlush(any());
    }
    @Test void completedEditCannotSubmitDraftOrAccessAnotherUsersSurvey() {
        assertThatThrownBy(() -> service.updateCompleted(http, body(fullAnswers(), null)))
                .isInstanceOf(FormValidationException.class);
        var draft = service.save(http, body(Map.of(), null), false);
        assertThatThrownBy(() -> service.updateCompleted(http, body(fullAnswers(), draft.revision())))
                .isInstanceOf(FormValidationException.class);
        service.save(http, body(fullAnswers(), draft.revision()), true);
        when(users.getTokenUser(http)).thenReturn(User.builder().userId(99L).build());
        assertThatThrownBy(() -> service.updateCompleted(http, body(fullAnswers(), null)))
                .isInstanceOf(FormValidationException.class);
        verify(repository, times(2)).saveAndFlush(any());
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
