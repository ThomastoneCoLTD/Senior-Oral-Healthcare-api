package com.kaii.dentix.global.config;

import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebSecurityConfigTest.SecurityProbeController.class)
@Import(WebSecurityConfig.class)
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @Test
    void administratorRegistrationRemainsPublic() throws Exception {
        mockMvc.perform(post("/admin/account"))
                .andExpect(status().isOk());
    }

    @Test
    void administratorAccountManagementRequiresSuperAdministrator() throws Exception {
        mockMvc.perform(get("/admin/account/list"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/account/list").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/account/list").with(user("super").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void billingExportRequiresAdministratorAuthentication() throws Exception {
        mockMvc.perform(get("/admin/billing/export/excel"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/billing/export/excel").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void rawDaeguChainProxyRequiresSuperAdministrator() throws Exception {
        mockMvc.perform(post("/daegu-chain/token-20/mint"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/daegu-chain/token-20/mint").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/daegu-chain/token-20/mint").with(user("super").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void corsAllowsConfiguredFrontendAndRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/admin/billing/export/excel")
                        .header(HttpHeaders.ORIGIN, "https://soh.thomabio.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://soh.thomabio.com"));

        mockMvc.perform(options("/admin/billing/export/excel")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class SecurityProbeController {

        @PostMapping("/admin/account")
        void registerAdministrator() {
        }

        @GetMapping("/admin/account/list")
        void listAdministrators() {
        }

        @GetMapping("/admin/billing/export/excel")
        void exportBilling() {
        }

        @PostMapping("/daegu-chain/token-20/mint")
        void mintToken() {
        }
    }
}
