package com.kaii.dentix.domain.user.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainAccountService;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainDidService;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.UserRewardWalletRepository;
import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.domain.user.domain.UserDaeguCredentialStatus;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
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
        service = new UserDaeguProvisioningService(
                daeguChainDidService,
                daeguChainAccountService,
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
        when(daeguChainDidService.issueLoginUserCredential(user))
                .thenAnswer(invocation -> {
                    user.updateDaeguCredential(
                            "issued.credential.jwt",
                            UserDaeguCredentialStatus.ISSUED,
                            null,
                            null
                    );
                    return null;
                });

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
        verify(daeguChainDidService).issueLoginUserCredential(user);
    }

    @Test
    void provisionForSignUpStoresCredentialJwtReturnedByDidServer() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "did": "did:mitum:minic:0x123",
                  "address": "0x123",
                  "credentialJwt": "credential.jwt.value"
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        service.provisionForSignUp(user);

        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        assertThat(user.getDaeguCredentialJwt()).isEqualTo("credential.jwt.value");
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.ISSUED);
        verify(daeguChainDidService, never()).issueLoginUserCredential(user);
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
                        "cid-account"
                ));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(daeguChainDidService.issueLoginUserCredential(user))
                .thenAnswer(invocation -> {
                    user.updateDaeguCredential(
                            "issued.credential.jwt",
                            UserDaeguCredentialStatus.ISSUED,
                            null,
                            null
                    );
                    return null;
                });

        String walletAddress = service.provisionForSignUp(user);

        assertThat(walletAddress).isEqualTo("0x-wallet");
        assertThat(user.getDaeguDid()).isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        ArgumentCaptor<UserRewardWallet> captor = ArgumentCaptor.forClass(UserRewardWallet.class);
        verify(userRewardWalletRepository).save(captor.capture());
        assertThat(captor.getValue().getDaeguDid()).isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(captor.getValue().getWalletAddress()).isEqualTo("0x-wallet");
        verify(daeguChainAccountService).createAccount(any());
    }

    @Test
    void provisionForSignUpMarksDidFailedWhenExternalApiFails() {
        User user = User.builder()
                .userId(7L)
                .build();
        when(daeguChainDidService.createAccount(any()))
                .thenThrow(new BadRequestApiException("token is required"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        String walletAddress = service.provisionForSignUp(user);

        assertThat(walletAddress).isNull();
        assertThat(user.getDaeguDid()).isNull();
        assertThat(user.getDaeguDidKey()).isNull();
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.FAILED);
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.FAILED);
        verify(userRewardWalletRepository, never()).save(any(UserRewardWallet.class));
        verify(daeguChainDidService, never()).issueLoginUserCredential(any(User.class));
    }

    @Test
    void provisionForSignUpDoesNotFailWhenWalletProvisioningFails() throws Exception {
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
        when(daeguChainAccountService.createAccount(any()))
                .thenThrow(new BadRequestApiException("token is required"));
        when(daeguChainDidService.issueLoginUserCredential(user))
                .thenAnswer(invocation -> {
                    user.updateDaeguCredential(
                            "issued.credential.jwt",
                            UserDaeguCredentialStatus.ISSUED,
                            null,
                            null
                    );
                    return null;
                });

        String walletAddress = service.provisionForSignUp(user);

        assertThat(walletAddress).isNull();
        assertThat(user.getDaeguDid()).isEqualTo("did:key:z6MkSelfGenerated");
        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.ISSUED);
        verify(userRewardWalletRepository, never()).save(any(UserRewardWallet.class));
        verify(daeguChainDidService).issueLoginUserCredential(user);
    }

    @Test
    void provisionForSignUpMarksCredentialFailedWhenCredentialIssueFails() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "did": "did:mitum:minic:0x123",
                  "address": "0x123"
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(daeguChainDidService.issueLoginUserCredential(user))
                .thenThrow(new BadRequestApiException("credential jwt is empty"));

        service.provisionForSignUp(user);

        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.FAILED);
    }

    @Test
    void provisionForSignUpMarksCredentialFailedWhenCredentialIssueDoesNotStoreJwt() throws Exception {
        User user = User.builder()
                .userId(7L)
                .userLoginIdentifier("soh-user-001")
                .build();
        JsonNode didData = new ObjectMapper().readTree("""
                {
                  "did": "did:mitum:minic:0x123",
                  "address": "0x123"
                }
                """);
        when(daeguChainDidService.createAccount(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", null, "", didData, "cid"));
        when(userRewardWalletRepository.findByUserId(7L)).thenReturn(Optional.empty());

        service.provisionForSignUp(user);

        assertThat(user.getDaeguDidStatus()).isEqualTo(UserDaeguIdentityStatus.ISSUED);
        assertThat(user.getDaeguCredentialStatus()).isEqualTo(UserDaeguCredentialStatus.FAILED);
    }
}
