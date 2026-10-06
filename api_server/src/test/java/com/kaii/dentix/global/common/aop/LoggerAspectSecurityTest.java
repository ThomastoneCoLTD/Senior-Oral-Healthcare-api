package com.kaii.dentix.global.common.aop;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.systemLog.dao.SystemLogRepository;
import com.kaii.dentix.domain.errorLog.dao.ErrorLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoggerAspectSecurityTest {
    @Test void nestedSecretsRecoveryAnswersAndPersonalFieldsAreRedacted() {
        var logger = new LoggerAspect(new ObjectMapper(), mock(JwtTokenUtil.class), mock(SystemLogRepository.class), mock(ErrorLogRepository.class));
        var input = Map.of("nested", List.of(Map.of("holder_pkey", "private-material", "mnemonic", "recovery-words", "findPwdAnswer", "personal-answer",
                "userPhoneNumber", "01012345678", "userName", "personal-name", "Cookie", "session-material")), "id", 7);
        String output = ReflectionTestUtils.invokeMethod(logger, "serializeForLog", input);
        assertThat(output).doesNotContain("private-material", "recovery-words", "personal-answer", "01012345678", "personal-name", "session-material")
                .contains("********", "\"id\":7");
    }
}
