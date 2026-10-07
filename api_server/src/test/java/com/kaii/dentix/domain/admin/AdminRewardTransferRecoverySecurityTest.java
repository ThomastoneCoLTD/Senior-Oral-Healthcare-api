package com.kaii.dentix.domain.admin;

import com.kaii.dentix.domain.admin.application.AdminRewardTransferRecoveryService;
import com.kaii.dentix.domain.admin.controller.AdminRewardTransferRecoveryController;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminRewardTransferRecoveryController.class)
@Import(WebSecurityConfig.class)
class AdminRewardTransferRecoverySecurityTest {
    @Autowired MockMvc mvc;
    @MockBean AdminRewardTransferRecoveryService service;
    @MockBean JwtTokenUtil jwt;

    @Test void bothRecoveryStagesRequireSuperAdmin() throws Exception {
        for (String action : new String[]{"preview", "confirm"}) {
            String path="/admin/daegu-chain/token/reward-transfers/10/recovery/"+action;
            String body="{\"factHash\":\""+"a".repeat(64)+"\"}";
            mvc.perform(post(path).contentType("application/json").content(body)).andExpect(status().isForbidden());
            for(String role:new String[]{"USER","ADMIN"}) {
                mvc.perform(post(path).with(user("1").roles(role)).contentType("application/json").content(body))
                        .andExpect(status().isForbidden());
            }
            verifyNoInteractions(service);
            mvc.perform(post(path).with(user("9").roles("SUPER_ADMIN")).contentType("application/json").content(body))
                    .andExpect(status().isOk());
            clearInvocations(service);
        }
    }
    @Test void invalidOrEmptyHashIsRejectedBeforeService() throws Exception {
        for(String body:new String[]{"{}","{\"factHash\":\"\"}","{\"factHash\":\"bad/hash\"}"}) {
            mvc.perform(post("/admin/daegu-chain/token/reward-transfers/10/recovery/confirm")
                    .with(user("9").roles("SUPER_ADMIN")).contentType("application/json").content(body))
                    .andExpect(jsonPath("$.rt").value(org.hamcrest.Matchers.not(200)));
        }
        verifyNoInteractions(service);
    }
}
