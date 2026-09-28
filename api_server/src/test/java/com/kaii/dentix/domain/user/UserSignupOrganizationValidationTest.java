package com.kaii.dentix.domain.user;

import com.kaii.dentix.domain.user.dto.UserDto;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserSignupOrganizationValidationTest {
    private static final Class<?>[] SIGNUP_TYPES = {
            UserDto.SignUpRequest.class, UserDto.DidSignUpRequest.class, UserDto.DadaeguSignUpRequest.class
    };

    @ParameterizedTest
    @ValueSource(strings = {"대구1", "대구2", "대구3", "기타_천안"})
    void acceptsSupportedOrganizationsForEverySignupPath(String organization) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Class<?> type : SIGNUP_TYPES) {
                assertThat(factory.getValidator().validateValue(type, "realOrganization", organization))
                        .as(type.getSimpleName()).isEmpty();
            }
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "기타", "천안", "기타_천안2", "대구4"})
    void rejectsMissingAndUnsupportedOrganizationsForEverySignupPath(String organization) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Class<?> type : SIGNUP_TYPES) {
                assertThat(factory.getValidator().validateValue(type, "realOrganization", organization))
                        .as(type.getSimpleName()).isNotEmpty();
            }
        }
    }
}
