package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class IntakeSurveyDto {
    public record Option(int value, String label) {}
    public record Condition(String key, int value) {}
    public record Question(String key, String text, String type, boolean required,
                           List<Option> options, Condition showWhen, String help) {}
    public record Section(int number, String title, String description, List<Question> questions, String source) {}
    public record Template(String version, List<Section> sections) {}
    public record SaveRequest(@NotBlank String version, @NotNull @Size(max = 47) Map<String, JsonNode> surveyAnswers,
                              @Min(1) @Max(7) int currentTab, Long revision) {}
    public record Status(boolean required, boolean completed, Instant completedAt) {}
    public record State(Template template, boolean completed, Instant completedAt, int currentTab,
                        Map<String, JsonNode> surveyAnswers, Map<String, Integer> surveyScores, Long revision) {}
}
