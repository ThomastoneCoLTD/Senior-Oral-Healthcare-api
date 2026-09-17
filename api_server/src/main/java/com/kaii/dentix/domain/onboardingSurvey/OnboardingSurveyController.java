package com.kaii.dentix.domain.onboardingSurvey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static com.kaii.dentix.domain.onboardingSurvey.OnboardingSurveyDto.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding-survey")
public class OnboardingSurveyController {
    private final OnboardingSurveyService service;
    private final OnboardingSurveyTemplate template;
    private final UserService userService;

    @GetMapping("/status")
    public DataResponse<Status> status(HttpServletRequest request) {
        return new DataResponse<>(service.status(request));
    }

    @GetMapping("/template")
    public DataResponse<Template> template(HttpServletRequest request) {
        userService.getTokenUser(request);
        return new DataResponse<>(template.get());
    }

    @PostMapping("/submit")
    public DataResponse<SubmitResponse> submit(HttpServletRequest request, @RequestBody SubmitRequest body)
            throws JsonProcessingException {
        return new DataResponse<>(service.submit(request, body));
    }

    @ExceptionHandler(SurveyValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationError invalid(SurveyValidationException exception) {
        return new ValidationError(400, exception.getMessage(), exception.getQuestionKey());
    }
}
