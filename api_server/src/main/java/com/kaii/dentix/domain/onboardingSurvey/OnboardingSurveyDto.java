package com.kaii.dentix.domain.onboardingSurvey;

import java.util.List;
import java.util.Map;

public final class OnboardingSurveyDto {
    private OnboardingSurveyDto() {}
    public record Option(String value, String label) {}
    public record Question(String key, String title, String type, List<Option> options,
                           String dependsOn, String dependsValue) {}
    public record Section(String key, String title, String description, List<Question> questions) {}
    public record Template(String version, String title, List<Section> sections) {}
    public record Status(boolean required, boolean completed) {}
    public record SubmitRequest(String version, Map<String, List<String>> answers) {}
    public record SubmitResponse(boolean completed) {}
    public record ValidationError(int rt, String rtMsg, String questionKey) {}
}
