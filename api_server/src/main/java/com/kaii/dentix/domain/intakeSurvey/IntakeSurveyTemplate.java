package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import static com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;

@Component
public class IntakeSurveyTemplate {
    private final Template template;

    public IntakeSurveyTemplate(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("template/intake-survey.json").getInputStream()) {
            template = mapper.readValue(input, Template.class);
        }
    }

    public Template get() { return template; }

    public Map<String, JsonNode> validate(SaveRequest request, boolean complete) {
        if (!template.version().equals(request.version()) || request.currentTab() < 1 || request.currentTab() > 7
                || request.surveyAnswers() == null || request.surveyAnswers().size() > 47) {
            throw new FormValidationException("설문 양식을 다시 불러와 주세요.");
        }
        Map<String, JsonNode> answers = new LinkedHashMap<>(request.surveyAnswers());
        Set<String> keys = new HashSet<>();
        for (Section section : template.sections()) {
            for (Question question : section.questions()) {
                keys.add(question.key());
                if (question.showWhen() != null && !matches(question.showWhen(), answers)) {
                    answers.remove(question.key());
                    continue;
                }
                JsonNode value = answers.get(question.key());
                if (value == null || value.isNull() || (value.isTextual() && value.textValue().isBlank())) {
                    answers.remove(question.key());
                    if (complete && question.required()) invalid(section, question);
                    continue;
                }
                switch (question.type()) {
                    case "single" -> { if (!isOption(question, value)) invalid(section, question); }
                    case "multiple" -> {
                        if (!value.isArray() || value.size() > question.options().size()) invalid(section, question);
                        Set<Integer> selected = new HashSet<>();
                        for (JsonNode option : value) {
                            if (!isOption(question, option) || !selected.add(option.intValue())) invalid(section, question);
                        }
                    }
                    case "month" -> {
                        if (!value.isTextual() || !value.textValue().matches("\\d{4}-(0[1-9]|1[0-2])")) invalid(section, question);
                        YearMonth month = YearMonth.parse(value.textValue());
                        if (month.getYear() < 1900 || month.isAfter(YearMonth.now(ZoneId.of("Asia/Seoul")))) invalid(section, question);
                    }
                    default -> throw new IllegalStateException("Unknown survey question type");
                }
            }
        }
        if (!keys.containsAll(answers.keySet())) throw new FormValidationException("등록되지 않은 설문 문항입니다.");
        return answers;
    }

    public Map<String, Integer> scores(Map<String, JsonNode> answers) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        // Preserve the source's option scores; do not generate a medical diagnosis.
        for (Section section : template.sections()) {
            if (section.number() <= 6) {
                scores.put(String.valueOf(section.number()), section.questions().stream()
                        .mapToInt(question -> answers.get(question.key()).intValue()).sum());
            }
        }
        return scores;
    }

    private boolean matches(Condition condition, Map<String, JsonNode> answers) {
        JsonNode value = answers.get(condition.key());
        return value != null && value.isIntegralNumber() && value.canConvertToInt() && value.intValue() == condition.value();
    }

    private boolean isOption(Question question, JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToInt()
                && question.options().stream().anyMatch(option -> option.value() == value.intValue());
    }

    private void invalid(Section section, Question question) {
        throw new FormValidationException(section.number() + "번 탭의 문항을 확인해 주세요: " + question.text());
    }
}
