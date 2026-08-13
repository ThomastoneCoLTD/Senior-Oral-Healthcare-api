package com.kaii.dentix.domain.findPwdController;

import com.kaii.dentix.domain.findPwdQuestion.application.FindIdQuestionInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FindIdQuestionInitializerTest {

    @Test
    void seedsNineStableRecoveryQuestions() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        FindIdQuestionInitializer initializer = new FindIdQuestionInitializer(jdbcTemplate);

        initializer.run(null);

        verify(jdbcTemplate, times(9)).update(anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
