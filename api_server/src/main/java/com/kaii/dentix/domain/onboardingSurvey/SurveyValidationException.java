package com.kaii.dentix.domain.onboardingSurvey;

import lombok.Getter;

@Getter
public class SurveyValidationException extends RuntimeException {
    private final String questionKey;
    public SurveyValidationException(String questionKey, String message) {
        super(message);
        this.questionKey = questionKey;
    }
}
