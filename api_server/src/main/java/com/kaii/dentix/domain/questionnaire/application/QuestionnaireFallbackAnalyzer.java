package com.kaii.dentix.domain.questionnaire.application;

import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireDto;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class QuestionnaireFallbackAnalyzer {
    public static final String MODEL_VERSION = "rules-v1";

    public QuestionnaireDto.AnalysisResponse analyze(Map<String, ?> survey) {
        Map<?, ?> form = survey != null && survey.get("form") instanceof Map<?, ?> value
                ? value
                : Map.of();
        Set<String> types = new LinkedHashSet<>();

        Integer age = singleValue(form.get("q_2"));
        if (Integer.valueOf(1).equals(age)) types.add("I");
        if (Integer.valueOf(2).equals(age)) types.add("J");
        if (Integer.valueOf(3).equals(age)) types.add("K");

        Set<Integer> symptoms = values(form.get("q_3"));
        if (symptoms.contains(3)) types.add("B");
        if (symptoms.contains(4)) types.add("D");
        if (symptoms.contains(5)) types.add("C");
        if (symptoms.contains(6) || symptoms.contains(7)) types.add("H");

        Set<Integer> treatments = values(form.get("q_8"));
        if (treatments.contains(3) || treatments.contains(4)) types.add("B");
        if (treatments.contains(5) || treatments.contains(6)) types.add("E");
        if (treatments.contains(7)) types.add("F");
        if (treatments.contains(8)) types.add("C");

        Integer selfAssessment = singleValue(form.get("q_1"));
        Integer brushingCount = singleValue(form.get("q_4"));
        Integer brushingEducation = singleValue(form.get("q_7"));
        if (isAtLeast(selfAssessment, 4)
                || isAtMost(brushingCount, 2)
                || Integer.valueOf(2).equals(brushingEducation)) {
            types.add("A");
        }

        Integer sweets = singleValue(form.get("q_9"));
        if (Integer.valueOf(3).equals(sweets) || Integer.valueOf(4).equals(sweets)) {
            types.add("B");
        }
        if (Integer.valueOf(2).equals(singleValue(form.get("q_10")))) {
            types.add("C");
        }

        if (types.isEmpty()) types.add("G");

        return QuestionnaireDto.AnalysisResponse.builder()
                .statusCode(200)
                .statusMsg("RULE_BASED_FALLBACK")
                .contentsType(types.stream().toList())
                .build();
    }

    private Set<Integer> values(Object value) {
        Set<Integer> result = new LinkedHashSet<>();
        if (value instanceof Number number) {
            result.add(number.intValue());
        } else if (value instanceof Collection<?> collection) {
            collection.stream()
                    .filter(Number.class::isInstance)
                    .map(Number.class::cast)
                    .map(Number::intValue)
                    .forEach(result::add);
        } else if (value instanceof Object[] array) {
            for (Object item : array) {
                if (item instanceof Number number) result.add(number.intValue());
            }
        }
        return result;
    }

    private Integer singleValue(Object value) {
        return values(value).stream().findFirst().orElse(null);
    }

    private boolean isAtLeast(Integer value, int threshold) {
        return value != null && value >= threshold;
    }

    private boolean isAtMost(Integer value, int threshold) {
        return value != null && value <= threshold;
    }
}
