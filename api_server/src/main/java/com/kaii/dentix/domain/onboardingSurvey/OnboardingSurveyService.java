package com.kaii.dentix.domain.onboardingSurvey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.global.common.error.exception.NotFoundDataException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.kaii.dentix.domain.onboardingSurvey.OnboardingSurveyDto.*;

@Service
@RequiredArgsConstructor
public class OnboardingSurveyService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final OnboardingSurveyRepository repository;
    private final OnboardingSurveyTemplate template;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public Status status(HttpServletRequest request) {
        var user = userService.getTokenUser(request);
        boolean completed = repository.existsById(user.getUserId());
        return new Status(Boolean.TRUE.equals(user.getOnboardingSurveyRequired()) && !completed, completed);
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmitResponse submit(HttpServletRequest httpRequest, SubmitRequest request) throws JsonProcessingException {
        Long userId = userService.getTokenUser(httpRequest).getUserId();
        var user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundDataException("회원이 존재하지 않습니다."));
        if (repository.existsById(userId)) return new SubmitResponse(true);
        if (!Boolean.TRUE.equals(user.getOnboardingSurveyRequired())) {
            throw new SurveyValidationException(null, "최초 건강 설문 대상 회원이 아닙니다.");
        }
        var answers = template.validate(request);
        repository.saveAndFlush(new OnboardingSurvey(userId, template.get().version(),
                mapper.writeValueAsString(answers), mapper.writeValueAsString(template.scores(answers))));
        return new SubmitResponse(true);
    }
}
