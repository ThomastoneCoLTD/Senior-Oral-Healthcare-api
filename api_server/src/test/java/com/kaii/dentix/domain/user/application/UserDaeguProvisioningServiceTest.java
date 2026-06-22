package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDaeguProvisioningServiceTest {

    private DaeguChainDidService daeguChainDidService;
    private DaeguChainAccountService daeguChainAccountService;
    private UserRewardWalletRepository userRewardWalletRepository;
    private UserDaeguProvisioningService service;

    @BeforeEach
    void setUp() {
        daeguChainDidService = mock(DaeguChainDidService.class);
        daeguChainAccountService = mock(DaeguChainAccountService.class);
        userRewardWalletRepository = mock(UserRewardWalletRepository.class);
        Environment environment = mock(Environment.class);
        service = new UserDaeguProvisioningService(
                daeguChainDidService,
                daeguChainAccountService,
                userRewardWalletRepository,
                environment
        );
    }

    @Test
    void provisionForSignUpStoresDidReturnedByExternalApi() throws Exception {
        User user = User.builder()
                .userId(7L)
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "did": "did:mitum:minic:0x123",
                  "public_key": "external-public-key"
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(daeguChainAccountService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>(
                        "OK",
                        null,
                        "",
                        new DaeguChainDto.KeyPairData(new DaeguChainDto.KeyPair(
                                "private-key",
                                "public-key",
                                "0x-wallet"
                        )),
                        "cid"
                ));

        service.provisionForSignUp(user);

        assertThat(user.getDaeguDid()).isEqualTo("did:mitum:minic:0x123");
        assertThat(user.getDaeguDidKey()).isEqualTo("external-public-key");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        verify(userRewardWalletRepository).save(any(UserRewardWallet.class));
    }

    @Test
    void provisionForSignUpDoesNotCreateLocalDidWhenExternalApiFails() {
        User user = User.builder()
                .userId(7L)
                .build();
        when(daeguChainDidService.createAccount(any()))
                .thenThrow(new BadRequestApiException("token is required"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        service.provisionForSignUp(user);

        assertThat(user.getDaeguDid()).isNull();
        assertThat(user.getDaeguDidKey()).isNull();
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.FAILED);
        verify(daeguChainAccountService, never()).createAccount(any());
        verify(userRewardWalletRepository, never()).save(any(UserRewardWallet.class));
    }
}
