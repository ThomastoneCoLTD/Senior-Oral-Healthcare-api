package com.kaii.dentix.domain.reward;

import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.reward.application.UserRewardService;
import com.kaii.dentix.domain.reward.application.UserWalletTokenBalanceService;
import com.kaii.dentix.domain.reward.controller.UserRewardController;
import com.kaii.dentix.domain.reward.dto.UserRewardDto;
import com.kaii.dentix.global.config.WebSecurityConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserRewardController.class, properties = {
        "jwt.accessTokenKey=test-access-key-for-reward-security-only-32",
        "jwt.refreshTokenKey=test-refresh-key-for-reward-security-only-32"
})
@Import({WebSecurityConfig.class, JwtTokenUtil.class})
class UserRewardSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean UserRewardService rewards;
    @MockBean UserWalletTokenBalanceService balances;
    @Autowired JwtTokenUtil jwt;
    @MockBean UserRepository users;
    @MockBean AdminRepository admins;

    @ParameterizedTest
    @CsvSource({
            "GET, /user/rewards/wallet",
            "POST, /user/rewards/wallet/connect",
            "GET, /user/rewards/transactions",
            "GET, /user/rewards/wallet/token-balances"
    })
    void everyRewardRouteRejectsAnonymousAndBothAdministratorRolesBeforeServiceCalls(String method, String path) throws Exception {
        mvc.perform(call(method, path)).andExpect(status().isForbidden());
        for (String role : List.of("ADMIN", "SUPER_ADMIN")) {
            mvc.perform(call(method, path).with(user("7").roles(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(rewards, balances);
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /user/rewards/wallet, walletAddress, user-wallet",
            "POST, /user/rewards/wallet/connect, walletAddress, user-wallet",
            "GET, /user/rewards/transactions, transactions, []",
            "GET, /user/rewards/wallet/token-balances, status, NOT_CONNECTED"
    })
    void userCanStillUseEveryRewardRoute(String method, String path, String field, String expected) throws Exception {
        var wallet = UserRewardDto.WalletResponse.builder().walletAddress("user-wallet").pointBalance(3L).build();
        when(rewards.getWallet(any())).thenReturn(wallet);
        when(rewards.connectWallet(any(), any())).thenReturn(wallet);
        when(rewards.getTransactions(any())).thenReturn(UserRewardDto.TransactionListResponse.builder().transactions(List.of()).build());
        when(balances.getBalances(any())).thenReturn(new UserWalletTokenBalanceService.BalanceResponse("NOT_CONNECTED", List.of()));
        var result = mvc.perform(call(method, path).with(user("7").roles("USER")))
                .andExpect(status().isOk());
        if (field.equals("transactions")) result.andExpect(jsonPath("$.response.transactions").isEmpty());
        else result.andExpect(jsonPath("$.response." + field).value(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /user/rewards/wallet",
            "POST, /user/rewards/wallet/connect",
            "GET, /user/rewards/transactions",
            "GET, /user/rewards/wallet/token-balances"
    })
    void signedTokensWithCollidingIdsRespectRolesAndRevokedUserSessions(String method, String path) throws Exception {
        User account = User.builder().userId(7L).build();
        when(users.findById(7L)).thenReturn(Optional.of(account));
        account.updateLogin(jwt.createToken(account, TokenType.RefreshToken));
        String userToken = jwt.createToken(account, TokenType.AccessToken);
        for (YnType superRole : List.of(YnType.N, YnType.Y)) {
            Admin admin = Admin.builder().adminId(7L).adminIsSuper(superRole).build();
            when(admins.findById(7L)).thenReturn(Optional.of(admin));
            admin.updateAdminLogin(jwt.createToken(admin, TokenType.RefreshToken));
            mvc.perform(call(method, path).header("Authorization", "Bearer " + jwt.createToken(admin, TokenType.AccessToken)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(rewards, balances);
        mvc.perform(call(method, path).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        clearInvocations(rewards, balances);
        account.logout();
        mvc.perform(call(method, path).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rewards, balances);
    }

    private MockHttpServletRequestBuilder call(String method, String path) {
        return request(HttpMethod.valueOf(method), path).contentType("application/json").content("{}");
    }
}
