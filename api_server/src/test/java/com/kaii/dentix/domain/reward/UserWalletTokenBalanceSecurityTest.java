package com.kaii.dentix.domain.reward;
import com.kaii.dentix.domain.reward.application.UserRewardService;
import com.kaii.dentix.domain.reward.application.UserWalletTokenBalanceService;
import com.kaii.dentix.domain.reward.controller.UserRewardController;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.global.config.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRewardController.class)
@Import(WebSecurityConfig.class)
class UserWalletTokenBalanceSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean UserRewardService rewards;
    @MockBean UserWalletTokenBalanceService balances;
    @MockBean JwtTokenUtil jwt;
    @Test void onlyUserAuthorityCanReadTheirOwnWalletBalance() throws Exception {
        String path = "/user/rewards/wallet/token-balances";
        mvc.perform(get(path)).andExpect(status().isForbidden());
        for (String role : new String[]{"ADMIN", "SUPER_ADMIN"}) {
            mvc.perform(get(path).with(user("7").roles(role))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(balances, rewards);
        when(balances.getBalances(any())).thenReturn(new UserWalletTokenBalanceService.BalanceResponse("NOT_CONNECTED", List.of()));
        mvc.perform(get(path).with(user("7").roles("USER"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value("NOT_CONNECTED"));
    }
}
