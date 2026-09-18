package com.kaii.dentix.domain.intakeSurvey;

import com.kaii.dentix.domain.intakeSurvey.controller.AdminIntakeSurveyController;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.global.config.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminIntakeSurveyController.class)
@Import(WebSecurityConfig.class)
class AdminIntakeSurveySecurityTest {
    @Autowired MockMvc mvc;
    @MockBean AdminIntakeSurveyService service;
    @MockBean JwtTokenUtil jwt;
    @Test void anonymousUsersAndNormalAdminsCannotReadOrEditAnySurvey() throws Exception {
        mvc.perform(get("/admin/intake-surveys")).andExpect(status().isForbidden());
        for (String role : new String[]{"USER", "ADMIN"}) {
            mvc.perform(get("/admin/intake-surveys").with(user("test").roles(role))).andExpect(status().isForbidden());
            mvc.perform(get("/admin/intake-surveys/42").with(user("test").roles(role))).andExpect(status().isForbidden());
            mvc.perform(put("/admin/intake-surveys/42").with(user("test").roles(role))
                .contentType("application/json").content("{}")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
    @Test void superAdminCanReadAndUpdateTheRequestedUser() throws Exception {
        mvc.perform(get("/admin/intake-surveys").with(user("root").roles("SUPER_ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/intake-surveys/42").with(user("root").roles("SUPER_ADMIN"))).andExpect(status().isOk());
        mvc.perform(put("/admin/intake-surveys/42").with(user("root").roles("SUPER_ADMIN"))
            .contentType("application/json").content("{\"version\":\"v1\",\"surveyAnswers\":{},\"currentTab\":1,\"revision\":2}"))
            .andExpect(status().isOk());
        verify(service).get(42L);
        verify(service).update(eq(42L), any());
    }
}
