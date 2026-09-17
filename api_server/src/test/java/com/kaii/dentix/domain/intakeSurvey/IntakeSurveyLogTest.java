package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.global.common.aop.LoggerAspect;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class IntakeSurveyLogTest {
    @Test void healthAnswersAndScoresAreMaskedInNestedAuditPayloads() {
        var aspect = new LoggerAspect(new ObjectMapper(), null, null, null);
        String log = ReflectionTestUtils.invokeMethod(aspect, "serializeForLog", Map.of("response", Map.of(
                "surveyAnswers", Map.of("eat10_1", 4), "surveyScores", Map.of("1", 40), "completed", true)));
        assertThat(log).contains("********", "completed").doesNotContain("eat10_1", "40");
    }
}
