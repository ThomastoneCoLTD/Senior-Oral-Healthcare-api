package com.kaii.dentix.domain.intakeSurvey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.global.common.error.exception.FormValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Objects;
import static com.kaii.dentix.domain.intakeSurvey.IntakeSurveyDto.*;

@Service
@RequiredArgsConstructor
public class IntakeSurveyService {
    private final UserService userService;
    private final IntakeSurveyRepository repository;
    private final IntakeSurveyTemplate template;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public Status status(HttpServletRequest request) {
        var survey = repository.findById(userService.getTokenUser(request).getUserId()).orElse(null);
        boolean completed = survey != null && survey.getCompletedAt() != null;
        return new Status(!completed, completed, completed ? survey.getCompletedAt() : null);
    }

    @Transactional(readOnly = true)
    public State get(HttpServletRequest request) {
        return state(repository.findById(userService.getTokenUser(request).getUserId()).orElse(null));
    }

    @Transactional
    public State save(HttpServletRequest request, SaveRequest body, boolean complete) {
        Long userId = userService.getTokenUser(request).getUserId();
        repository.lockUser(userId);
        var survey = repository.findById(userId).orElseGet(() -> new UserIntakeSurvey(userId));
        // Retries after a lost submission response must not create or overwrite a completion.
        if (survey.getCompletedAt() != null) return state(survey);
        if (!Objects.equals(body.revision(), survey.getRevision())) {
            throw new FormValidationException("다른 화면에서 설문이 변경되었습니다. 새로고침 후 이어서 작성해 주세요.");
        }
        var answers = template.validate(body, complete);
        try {
            survey.save(template.get().version(), mapper.writeValueAsString(answers),
                    mapper.writeValueAsString(complete ? template.scores(answers) : Map.of()), body.currentTab(), complete);
            return state(repository.saveAndFlush(survey));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize intake survey", exception);
        }
    }

    State state(UserIntakeSurvey survey) {
        if (survey == null) return new State(template.get(), false, null, 1, Map.of(), Map.of(), null);
        try {
            return new State(template.get(), survey.getCompletedAt() != null, survey.getCompletedAt(), survey.getCurrentTab(),
                    mapper.readValue(survey.getAnswersJson(), new TypeReference<>() {}),
                    mapper.readValue(survey.getScoresJson(), new TypeReference<>() {}), survey.getRevision());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read intake survey", exception);
        }
    }
}
