package com.kaii.dentix.domain.oralExercise;
import com.kaii.dentix.domain.oralExercise.controller.OralExerciseHistoryController;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService;
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
@WebMvcTest(OralExerciseHistoryController.class)
@Import(WebSecurityConfig.class)
class OralExerciseHistorySecurityTest {
    @Autowired MockMvc mvc;
    @MockBean OralExerciseHistoryService service;
    @MockBean JwtTokenUtil jwt;
    @Test void historyIsSuperAdminOnlyIncludingDirectDetailUrls() throws Exception {
        for (String path : new String[]{"/admin/oral-exercise-history/users", "/admin/oral-exercise-history/users/1", "/admin/oral-exercise-history/users/1/2"}) {
            mvc.perform(get(path)).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("u").roles("USER"))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("a").roles("ADMIN"))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("s").roles("SUPER_ADMIN"))).andExpect(status().isOk());
        }
    }
    @Test void selfHistoryAndFailureCannotBeAccessedByAdmin() throws Exception {
        mvc.perform(get("/oral-exercise/history").with(user("a").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/oral-exercise/failures").with(user("a").roles("SUPER_ADMIN")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void regularUserCanReadOwnHistory() throws Exception {
        mvc.perform(get("/oral-exercise/history").with(user("u").roles("USER"))).andExpect(status().isOk());
    }
}
