package com.kaii.dentix.domain.onboardingSurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.user.application.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.kaii.dentix.domain.onboardingSurvey.OnboardingSurveyDto.*;

class OnboardingSurveyControllerTest {
    @Test void returnsFirstInvalidKeyWith400ForClientFocus() throws Exception {
        var service = mock(OnboardingSurveyService.class);
        when(service.submit(any(), any())).thenThrow(new SurveyValidationException("eat10_2", "응답해 주세요."));
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingSurveyController(service,
                new OnboardingSurveyTemplate(new ObjectMapper()), mock(UserService.class))).build();
        mvc.perform(post("/onboarding-survey/submit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"2026-09-17-v1\",\"answers\":{}}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.questionKey").value("eat10_2"))
                .andExpect(jsonPath("$.rt").value(400));
    }
    @Test void statusAndTemplateUseExistingResponseEnvelope() throws Exception {
        var service = mock(OnboardingSurveyService.class);
        when(service.status(any())).thenReturn(new Status(true, false));
        var mvc = MockMvcBuilders.standaloneSetup(new OnboardingSurveyController(service,
                new OnboardingSurveyTemplate(new ObjectMapper()), mock(UserService.class))).build();
        mvc.perform(get("/onboarding-survey/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.rt").value(200)).andExpect(jsonPath("$.response.required").value(true));
        mvc.perform(get("/onboarding-survey/template")).andExpect(status().isOk())
                .andExpect(jsonPath("$.response.sections.length()").value(7));
    }
}
