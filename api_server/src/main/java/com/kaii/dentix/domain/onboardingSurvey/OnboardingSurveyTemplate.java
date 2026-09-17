package com.kaii.dentix.domain.onboardingSurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import static com.kaii.dentix.domain.onboardingSurvey.OnboardingSurveyDto.*;

@Component
public class OnboardingSurveyTemplate {
    private final Template template;

    public OnboardingSurveyTemplate(ObjectMapper mapper) throws IOException {
        try (var stream = new ClassPathResource("template/onboarding-survey.json").getInputStream()) {
            template = mapper.readValue(stream, Template.class);
        }
    }

    public Template get() { return template; }

    public Map<String, List<String>> validate(SubmitRequest request) {
        if (request == null || !template.version().equals(request.version())) {
            throw new SurveyValidationException(null, "설문 버전이 변경되었습니다. 새로고침 후 다시 작성해 주세요.");
        }
        Map<String, List<String>> answers = request.answers() == null ? Map.of() : request.answers();
        Set<String> knownKeys = new HashSet<>();
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (Section section : template.sections()) {
            for (Question question : section.questions()) {
                knownKeys.add(question.key());
                if (question.dependsOn() != null && !List.of(question.dependsValue()).equals(answers.get(question.dependsOn()))) {
                    continue;
                }
                List<String> values = answers.get(question.key());
                if (values == null || values.isEmpty()) fail(question, "응답해 주세요.");
                if (values.stream().anyMatch(v -> v == null || v.isBlank())) fail(question, "응답해 주세요.");
                if ((!"multi".equals(question.type()) && values.size() != 1)
                        || new HashSet<>(values).size() != values.size()) fail(question, "선택 항목을 확인해 주세요.");
                if ("month".equals(question.type())) {
                    validateMonth(question, values.get(0));
                } else {
                    Set<String> allowed = new HashSet<>();
                    question.options().forEach(option -> allowed.add(option.value()));
                    if (!allowed.containsAll(values)) fail(question, "선택 항목을 확인해 주세요.");
                    if (values.contains("none") && values.size() > 1) fail(question, "어려움 없음은 단독으로 선택해 주세요.");
                }
                normalized.put(question.key(), List.copyOf(values));
            }
        }
        if (!knownKeys.containsAll(answers.keySet())) {
            throw new SurveyValidationException(null, "설문에 없는 문항이 포함되어 있습니다.");
        }
        return normalized;
    }

    private void validateMonth(Question question, String value) {
        if (question.options().stream().anyMatch(option -> option.value().equals(value))) return;
        try {
            if (!value.matches("[0-9]{4}-[0-9]{2}")) fail(question, "연도와 월을 입력해 주세요.");
            YearMonth month = YearMonth.parse(value);
            if (month.getYear() < 1900 || month.isAfter(YearMonth.now(ZoneId.of("Asia/Seoul")))) {
                fail(question, "1900년부터 현재 월 사이로 입력해 주세요.");
            }
        } catch (DateTimeParseException exception) {
            fail(question, "올바른 연도와 월을 입력해 주세요.");
        }
    }

    public Map<String, Integer> scores(Map<String, List<String>> answers) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Section section : template.sections()) {
            if ("dental".equals(section.key())) continue;
            int sum = section.questions().stream().mapToInt(q -> Integer.parseInt(answers.get(q.key()).get(0))).sum();
            scores.put(section.key(), sum);
        }
        return scores;
    }

    private void fail(Question question, String message) {
        throw new SurveyValidationException(question.key(), question.title() + " " + message);
    }
}
