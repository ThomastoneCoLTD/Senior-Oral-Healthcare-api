package com.kaii.dentix.domain.questionnaire;

import com.kaii.dentix.domain.questionnaire.application.QuestionnaireFallbackAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionnaireFallbackAnalyzerTest {
    private final QuestionnaireFallbackAnalyzer analyzer = new QuestionnaireFallbackAnalyzer();

    @Test
    void derivesPersonalizedTypesFromQuestionnaireAnswers() {
        Map<String, Object> survey = Map.of("form", Map.of(
                "q_1", 4,
                "q_2", 3,
                "q_3", List.of(4, 5, 7),
                "q_4", 1,
                "q_7", 2,
                "q_8", List.of(6, 7),
                "q_9", 4,
                "q_10", 2
        ));

        var response = analyzer.analyze(survey);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getStatusMsg()).isEqualTo("RULE_BASED_FALLBACK");
        assertThat(response.getContentsType())
                .containsExactly("K", "D", "C", "H", "E", "F", "A", "B");
    }

    @Test
    void returnsGeneralCareWhenNoRiskRuleMatches() {
        Map<String, Object> survey = Map.of("form", Map.of(
                "q_1", 1,
                "q_2", 4,
                "q_3", List.of(1),
                "q_4", 3,
                "q_7", 1,
                "q_8", List.of(9),
                "q_9", 1,
                "q_10", 1
        ));

        assertThat(analyzer.analyze(survey).getContentsType()).containsExactly("G");
    }
}
