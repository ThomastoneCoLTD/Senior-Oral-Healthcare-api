package com.kaii.dentix.domain.intakeSurvey;

import com.kaii.dentix.domain.intakeSurvey.controller.IntakeSurveyController;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.global.config.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntakeSurveyController.class)
@Import(WebSecurityConfig.class)
class IntakeSurveySecurityTest {
    @Autowired MockMvc mvc;
    @MockBean IntakeSurveyService service;
    @MockBean JwtTokenUtil jwt;

    @Test void anonymousIsRejected() throws Exception {
        mvc.perform(get("/user/intake-survey/status")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void adminsCannotAccessTheGeneralUserSurvey() throws Exception {
        mvc.perform(get("/user/intake-survey").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/user/intake-survey/submit").with(user("root").roles("SUPER_ADMIN"))
                .contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void userCanCheckRequiredStatus() throws Exception {
        when(service.status(any())).thenReturn(new IntakeSurveyDto.Status(true,false,null));
        mvc.perform(get("/user/intake-survey/status").with(user("user").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response.required").value(true));
    }
}
