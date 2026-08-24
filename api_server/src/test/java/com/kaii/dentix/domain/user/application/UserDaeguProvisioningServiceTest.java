package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.application.DaeguRewardWalletProvisioningService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserDaeguProvisioningServiceTest {

    private DaeguChainDidService daeguChainDidService;
    private DaeguRewardWalletProvisioningService rewardWalletProvisioningService;
    private UserRewardWalletRepository userRewardWalletRepository;
    private UserDaeguProvisioningService service;

    @BeforeEach
    void setUp() {
        daeguChainDidService = mock(DaeguChainDidService.class);
        rewardWalletProvisioningService = mock(DaeguRewardWalletProvisioningService.class);
        userRewardWalletRepository = mock(UserRewardWalletRepository.class);
        service = new UserDaeguProvisioningService(
                daeguChainDidService,
                rewardWalletProvisioningService,
                userRewardWalletRepository
        );
    }

    @Test
    void provisionForSignUpStoresDidReturnedByExternalApi() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "key_pair": {
                    "privatekey": "private-key",
                    "publickey": "external-public-key",
                    "address": "0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca"
                  },
                  "did": "did:mitum:minic:0x123",
                  "faucet": {
                    "currency": "DMC",
                    "amount": "1"
                  },
                  "tx": {
                    "hash": "4WQGWmrgKRp7Xx6x8xtMMaodJMm2UgYv7eQoHbhGV6Sn",
                    "fact_hash": "E5gAWQwvCgC3ZdLKmwrsg1KoZ4bFMSi1chGfAu5SnrYM"
                  }
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        String walletAddress = service.provisionForSignUp(user);

        assertThat(walletAddress).isEqualTo("0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca");
        assertThat(user.getDaeguDid()).isEqualTo("did:mitum:minic:0x123");
        assertThat(user.getDaeguDidKey()).isEqualTo("external-public-key");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        ArgumentCaptor<Map<String, Object>> createRequestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(daeguChainDidService).createAccount(createRequestCaptor.capture());
        assertThat(createRequestCaptor.getValue())
                .containsEntry("label", "soh-user-001")
                .doesNotContainKey("userIdentifier")
                .doesNotContainKey("userLoginIdentifier");
        ArgumentCaptor<UserRewardWallet> captor = ArgumentCaptor.forClass(UserRewardWallet.class);
        verify(userRewardWalletRepository).save(captor.capture());
        assertThat(captor.getValue().getWalletAddress()).isEqualTo("0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca");
        verify(rewardWalletProvisioningService).approveActivatedWallet(
                7L,
                "0x3e33E1C95833809532A08f84b0A145277AFC1eA9fca",
                "private-key"
        );
    }

    @Test
    void provisionForSignUpCreatesWalletWhenDidServerDoesNotReturnAddress() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "did": "did:key:z6MkSelfGenerated"
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(rewardWalletProvisioningService.createActivatedWallet(7L)).thenReturn("0x-wallet");
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        String walletAddress = service.provisionForSignUp(user);

        assertThat(walletAddress).isEqualTo("0x-wallet");
        assertThat(user.getDaeguDid()).isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        ArgumentCaptor<UserRewardWallet> captor = ArgumentCaptor.forClass(UserRewardWallet.class);
        verify(userRewardWalletRepository).save(captor.capture());
        assertThat(captor.getValue().getDaeguDid()).isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(captor.getValue().getWalletAddress()).isEqualTo("0x-wallet");
        verify(rewardWalletProvisioningService).createActivatedWallet(7L);
    }

    @Test
    void provisionForSignUpFailsWhenDidProvisioningFails() {
        User user = User.builder()
                .userId(7L)
                .build();
        when(daeguChainDidService.createAccount(any()))
                .thenThrow(new BadRequestApiException("token is required"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.provisionForSignUp(user))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("Daegu DID provisioning failed");
        assertThat(user.getDaeguDid()).isNull();
        assertThat(user.getDaeguDidKey()).isNull();
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.FAILED);
        verify(userRewardWalletRepository, never()).save(any(UserRewardWallet.class));
    }

    @Test
    void provisionForSignUpFailsWhenWalletProvisioningFails() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "did": "did:key:z6MkSelfGenerated"
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(rewardWalletProvisioningService.createActivatedWallet(7L))
                .thenThrow(new BadRequestApiException("token is required"));

        assertThatThrownBy(() -> service.provisionForSignUp(user))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("token is required");
        assertThat(user.getDaeguDid()).isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        verify(userRewardWalletRepository, never()).save(any(UserRewardWallet.class));
    }

    @Test
    void ensureProvisionedReusesExistingDidAndWalletWithoutCreatingAnotherAccount() {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("local-user")
                .daeguDid("did:key:existing")
                .daeguDidStatus(UserDaeguIdentityStatus.ISSUED)
                .build();
        UserRewardWallet wallet = UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:key:existing")
                .walletAddress("0x-existing-wallet")
                .build();
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.of(wallet));

        String walletAddress = service.ensureProvisioned(user);

        assertThat(walletAddress).isEqualTo("0x-existing-wallet");
        verify(daeguChainDidService, never()).createAccount(any());
        verifyNoInteractions(rewardWalletProvisioningService);
        verify(userRewardWalletRepository).save(wallet);
    }

    @Test
    void provisionForDadaeguReusesLocalUsersWalletAndKeepsSelfIssuedDid() {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("local-user")
                .daeguDid("did:key:self-issued")
                .daeguDidStatus(UserDaeguIdentityStatus.ISSUED)
                .build();
        UserRewardWallet wallet = UserRewardWallet.builder()
                .userId(7L)
                .pointBalance(0L)
                .daeguDid("did:key:self-issued")
                .walletAddress("0x-existing-wallet")
                .build();
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.of(wallet));

        String walletAddress = service.provisionForDadaegu(user, "did:daegu:external-user");

        assertThat(walletAddress).isEqualTo("0x-existing-wallet");
        assertThat(user.getDaeguDid()).isEqualTo("did:key:self-issued");
        assertThat(wallet.getDaeguDid()).isEqualTo("did:daegu:external-user");
        assertThat(wallet.getWalletAddress()).isEqualTo("0x-existing-wallet");
        verify(daeguChainDidService, never()).createAccount(any());
        verifyNoInteractions(rewardWalletProvisioningService);
        verify(userRewardWalletRepository).save(wallet);
    }

    @Test
    void provisionForDadaeguOnlyUserUsesExternalDidAndCreatesWalletWithoutSelfDid() {
        User user = User.builder()
                .userId(8L)
                .userLoginIdentifier("dg-user")
                .build();
        when(userRewardWalletRepository.findByUserId(8L)).thenReturn(Optional.empty());
        when(rewardWalletProvisioningService.createActivatedWallet(8L)).thenReturn("0x-new-wallet");

        String walletAddress = service.provisionForDadaegu(user, "did:daegu:new-user");

        assertThat(walletAddress).isEqualTo("0x-new-wallet");
        assertThat(user.getDaeguDid()).isEqualTo("did:daegu:new-user");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        verify(daeguChainDidService, never()).createAccount(any());
        ArgumentCaptor<UserRewardWallet> walletCaptor = ArgumentCaptor.forClass(UserRewardWallet.class);
        verify(userRewardWalletRepository).save(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getDaeguDid()).isEqualTo("did:daegu:new-user");
        assertThat(walletCaptor.getValue().getWalletAddress()).isEqualTo("0x-new-wallet");
        verify(rewardWalletProvisioningService).createActivatedWallet(8L);
    }

    @Test
    void provisionForDadaeguDoesNotStoreWalletWhenReclaimApprovalFails() {
        User user = User.builder().userId(8L).userLoginIdentifier("dg-user").build();
        when(userRewardWalletRepository.findByUserId(8L)).thenReturn(Optional.empty());
        when(rewardWalletProvisioningService.createActivatedWallet(8L))
                .thenThrow(new BadRequestApiException(
                        "DaeguChain reward reclaim approval failed. state=OOPS, msg=approval failed"
                ));

        assertThatThrownBy(() -> service.provisionForDadaegu(user, "did:daegu:new-user"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("DaeguChain reward reclaim approval failed")
                .hasMessageContaining("state=OOPS");
        verify(userRewardWalletRepository, never()).save(any(UserRewardWallet.class));
    }
}
