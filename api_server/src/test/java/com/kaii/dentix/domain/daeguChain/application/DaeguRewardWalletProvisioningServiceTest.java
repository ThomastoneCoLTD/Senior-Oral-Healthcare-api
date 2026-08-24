package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DaeguRewardWalletProvisioningServiceTest {

    private DaeguChainAccountService accountService;
    private DaeguChainToken20Service token20Service;
    private DaeguWalletPrivateKeyCipher privateKeyCipher;
    private DaeguRewardWalletProvisioningService service;

    @BeforeEach
    void setUp() {
        accountService = mock(DaeguChainAccountService.class);
        token20Service = mock(DaeguChainToken20Service.class);
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setTokenOwnerAddress("0x-owner");
        properties.setWalletEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        privateKeyCipher = new DaeguWalletPrivateKeyCipher(properties);
        service = new DaeguRewardWalletProvisioningService(
                accountService,
                token20Service,
                properties,
                privateKeyCipher
        );
    }

    @Test
    void activatesNewAddressAndEncryptsSigningKeyWithoutPrematureApproval() {
        when(accountService.createAccount(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OK",
                Map.of(),
                "",
                new DaeguChainDto.KeyPairData(new DaeguChainDto.KeyPair(
                        "private-key",
                        "public-key",
                        "0x-wallet"
                )),
                "cid-create"
        ));
        when(accountService.faucet(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OK", Map.of(), "", null, "cid-faucet"
        ));
        DaeguRewardWalletProvisioningService.ProvisionedWallet wallet = service.createActivatedWallet(7L);

        assertThat(wallet.walletAddress()).isEqualTo("0x-wallet");
        assertThat(wallet.privateKeyCiphertext()).startsWith("v1:");
        assertThat(wallet.privateKeyCiphertext()).doesNotContain("private-key");
        assertThat(privateKeyCipher.decrypt(wallet.privateKeyCiphertext())).isEqualTo("private-key");
        verify(accountService).createAccount(any());

        ArgumentCaptor<DaeguChainDto.AccountAddressRequest> faucetCaptor =
                ArgumentCaptor.forClass(DaeguChainDto.AccountAddressRequest.class);
        verify(accountService).faucet(faucetCaptor.capture());
        assertThat(faucetCaptor.getValue().getAddress()).isEqualTo("0x-wallet");

        verifyNoInteractions(token20Service);
    }

    @Test
    void doesNotApproveWhenWalletActivationFails() {
        when(accountService.createAccount(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OK",
                Map.of(),
                "",
                new DaeguChainDto.KeyPairData(new DaeguChainDto.KeyPair(
                        "private-key",
                        "public-key",
                        "0x-wallet"
                )),
                "cid-create"
        ));
        when(accountService.faucet(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OOPS",
                Map.of("bcode", "B0593", "gcode", "P06D502"),
                "account activation failed",
                null,
                "cid-faucet"
        ));

        assertThatThrownBy(() -> service.createActivatedWallet(7L))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("DaeguChain reward wallet activation failed")
                .hasMessageContaining("P06D502");

        verifyNoInteractions(token20Service);
    }

    @Test
    void surfacesApprovalFailureDetails() {
        when(token20Service.approveToken(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OOPS",
                Map.of("bcode", "B0593", "gcode", "P06D999"),
                "approval operation failed",
                (JsonNode) null,
                "cid-approve"
        ));

        String ciphertext = privateKeyCipher.encrypt("private-key");
        assertThatThrownBy(() -> service.approveRewardContract(
                7L, "0x-contract", "0x-wallet", ciphertext
        ))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("DaeguChain reward reclaim approval failed")
                .hasMessageContaining("approval operation failed")
                .hasMessageContaining("P06D999");
    }

    @Test
    void approvesOnlyTheRewardContractThatAlreadyHasTokenBalance() {
        when(token20Service.approveToken(any()))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", Map.of(), "", null, "cid-approve"));

        service.approveRewardContract(
                7L,
                "0x-contract",
                "0x-wallet",
                privateKeyCipher.encrypt("private-key")
        );

        ArgumentCaptor<DaeguChainDto.TokenApproveRequest> captor =
                ArgumentCaptor.forClass(DaeguChainDto.TokenApproveRequest.class);
        verify(token20Service).approveToken(captor.capture());
        assertThat(captor.getValue().getContAddr()).isEqualTo("0x-contract");
        assertThat(captor.getValue().getHolder()).isEqualTo("0x-wallet");
        assertThat(captor.getValue().getHolderPkey()).isEqualTo("private-key");
        assertThat(captor.getValue().getApproved()).isEqualTo("0x-owner");
    }
}
