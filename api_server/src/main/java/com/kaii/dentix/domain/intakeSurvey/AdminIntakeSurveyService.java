package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import static com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;

@Service
@RequiredArgsConstructor
public class AdminIntakeSurveyService {
    private final AdminIntakeSurveyRepository users;
    private final IntakeSurveyRepository surveys;
    private final IntakeSurveyService reader;
    private final IntakeSurveyTemplate template;
    private final ObjectMapper mapper;

    public enum StatusFilter { ALL, NOT_STARTED, DRAFT, COMPLETED }
    public record Listing(List<AdminIntakeSurveyRepository.Summary> users, List<String> organizations,
                          long totalElements, int totalPages, int page) {}

    @Transactional(readOnly = true)
    public Listing list(String organization, String keyword, StatusFilter status, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || keyword.length() > 100) {
            throw new FormValidationException("조회 조건을 확인해 주세요.");
        }
        var result = users.search(organization == null ? null : organization.trim(), keyword.trim(),
                status.name(), PageRequest.of(page, size));
        return new Listing(result.getContent(), users.organizations(), result.getTotalElements(), result.getTotalPages(), page);
    }

    @Transactional(readOnly = true)
    public State get(Long userId) {
        if (!users.existsById(userId)) throw new FormValidationException("사용자를 찾을 수 없습니다.");
        return reader.state(surveys.findById(userId).orElse(null));
    }

    @Transactional
    public State update(Long userId, SaveRequest body) {
        if (surveys.lockUser(userId) == null) throw new FormValidationException("사용자를 찾을 수 없습니다.");
        var survey = surveys.findById(userId).orElseGet(() -> new UserIntakeSurvey(userId));
        if (!Objects.equals(body.revision(), survey.getRevision())) {
            throw new FormValidationException("다른 화면에서 설문이 변경되었습니다. 다시 조회한 뒤 수정해 주세요.");
        }
        // Editing preserves submission status; only the user submits an unfinished survey.
        boolean completed = survey.getCompletedAt() != null;
        var answers = template.validate(body, completed);
        try {
            survey.save(template.get().version(), mapper.writeValueAsString(answers),
                    mapper.writeValueAsString(completed ? template.scores(answers) : Map.of()), body.currentTab(), completed);
            return reader.state(surveys.saveAndFlush(survey));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize intake survey", e);
        }
    }
}
