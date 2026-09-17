package com.kaii.dentix.domain.onboardingSurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static com.kaii.dentix.domain.onboardingSurvey.OnboardingSurveyDto.*;

class OnboardingSurveyTemplateTest {
    private OnboardingSurveyTemplate template;
    @BeforeEach void setup() throws Exception { template = new OnboardingSurveyTemplate(new ObjectMapper()); }
    private Map<String, List<String>> complete() {
        Map<String, List<String>> answers = new LinkedHashMap<>();
        template.get().sections().forEach(s -> s.questions().forEach(q ->
                answers.put(q.key(), List.of("month".equals(q.type()) ? "2025-03" : q.options().get(0).value()))));
        answers.put("dental_1", List.of("1"));
        return answers;
    }
    private Map<String, List<String>> validate(Map<String, List<String>> answers) {
        return template.validate(new SubmitRequest(template.get().version(), answers));
    }
    @Test void all47QuestionsAreRequiredAndZeroIsAnAnswer() {
        var answers = complete();
        assertThat(validate(answers)).hasSize(47);
        for (String key : answers.keySet()) {
            var missing = new LinkedHashMap<>(answers); missing.remove(key);
            assertThatThrownBy(() -> validate(missing)).isInstanceOfSatisfying(SurveyValidationException.class,
                    e -> assertThat(e.getQuestionKey()).isEqualTo(key));
        }
    }
    @Test void rejectsNullEmptyDuplicateAndOutOfRangeValues() {
        for (List<String> invalid : Arrays.asList(null, List.<String>of(), Arrays.asList((String) null),
                List.of(""), List.of("5"), List.of("0", "0"))) {
            var answers = complete(); answers.put("eat10_1", invalid);
            assertThatThrownBy(() -> validate(answers)).isInstanceOf(SurveyValidationException.class);
        }
        var answers = complete(); answers.put("mnasf_4", List.of("1"));
        assertThatThrownBy(() -> validate(answers)).isInstanceOf(SurveyValidationException.class);
    }
    @Test void visitFrequencyIsConditionalAndStaleHiddenAnswersAreDiscarded() {
        var answers = complete(); answers.put("dental_1", List.of("0")); answers.put("dental_2", List.of("bad"));
        assertThat(validate(answers)).hasSize(46).doesNotContainKey("dental_2");
    }
    @Test void datesAndExplicitNonApplicableAnswersAreValidated() {
        for (String month : List.of("2025-13", "0000-01", "1899-12", "9999-12", "2025-1", "2025-01-01")) {
            var answers = complete(); answers.put("dental_3", List.of(month));
            assertThatThrownBy(() -> validate(answers)).isInstanceOf(SurveyValidationException.class);
        }
        for (String month : List.of("never", "unknown", "2025-03")) {
            var answers = complete(); answers.put("dental_3", List.of(month));
            assertThat(validate(answers)).containsEntry("dental_3", List.of(month));
        }
        var answers = complete(); answers.put("dental_5", List.of("none", "0"));
        assertThatThrownBy(() -> validate(answers)).isInstanceOf(SurveyValidationException.class);
        answers.put("dental_5", List.of("none")); assertThat(validate(answers)).hasSize(47);
        answers.put("dental_5", List.of("0", "1")); assertThat(validate(answers)).hasSize(47);
    }
    @Test void computesAllSixSectionScoresUsingPdfValues() {
        var answers = complete();
        template.get().sections().stream().filter(s -> !s.key().equals("dental")).forEach(s -> s.questions().forEach(q ->
                answers.put(q.key(), List.of(q.options().get(q.options().size() - 1).value()))));
        assertThat(template.scores(validate(answers))).containsExactlyInAnyOrderEntriesOf(
                Map.of("eat10", 40, "edsq", 12, "frail", 5, "aspiration", 4, "sarcf", 10, "mnasf", 14));
    }
    @Test void rejectsUnknownFieldsAndStaleTemplateVersion() {
        var answers = complete(); answers.put("extra", List.of("1"));
        assertThatThrownBy(() -> validate(answers)).isInstanceOf(SurveyValidationException.class);
        assertThatThrownBy(() -> template.validate(new SubmitRequest("old", complete()))).isInstanceOf(SurveyValidationException.class);
    }
}
