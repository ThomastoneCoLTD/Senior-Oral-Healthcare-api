package com.kaii.dentix.domain.daeguChain.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DaeguRewardWalletProvisioningServiceTest {

    private DaeguChainAccountService accountService;
    private DaeguChainToken20Service token20Service;
    private DaeguRewardWalletProvisioningService service;

    @BeforeEach
    void setUp() {
        accountService = mock(DaeguChainAccountService.class);
        token20Service = mock(DaeguChainToken20Service.class);
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setTokenOwnerAddress("0x-owner");
        properties.getRewardTokenContracts().put("ESSENTIAL_VIDEO_1", "0x-contract-1");
        properties.getRewardTokenContracts().put("OPTIONAL_VIDEO_1", "0x-contract-2");
        service = new DaeguRewardWalletProvisioningService(accountService, token20Service, properties);
    }

    @Test
    void activatesNewAddressBeforeApprovingRewardContracts() {
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
        when(token20Service.approveToken(any())).thenReturn(new DaeguChainDto.ApiResponse<>(
                "OK", Map.of(), "", null, "cid-approve"
        ));

        String walletAddress = service.createActivatedWallet(7L);

        assertThat(walletAddress).isEqualTo("0x-wallet");
        InOrder inOrder = inOrder(accountService, token20Service);
        inOrder.verify(accountService).createAccount(any());
        inOrder.verify(accountService).faucet(any());
        inOrder.verify(token20Service, times(2)).approveToken(any());

        ArgumentCaptor<DaeguChainDto.AccountAddressRequest> faucetCaptor =
                ArgumentCaptor.forClass(DaeguChainDto.AccountAddressRequest.class);
        verify(accountService).faucet(faucetCaptor.capture());
        assertThat(faucetCaptor.getValue().getAddress()).isEqualTo("0x-wallet");

        ArgumentCaptor<DaeguChainDto.TokenApproveRequest> approveCaptor =
                ArgumentCaptor.forClass(DaeguChainDto.TokenApproveRequest.class);
        verify(token20Service, times(2)).approveToken(approveCaptor.capture());
        assertThat(approveCaptor.getAllValues())
                .extracting(DaeguChainDto.TokenApproveRequest::getContAddr)
                .containsExactlyInAnyOrder("0x-contract-1", "0x-contract-2");
        assertThat(approveCaptor.getAllValues()).allSatisfy(request -> {
            assertThat(request.getHolder()).isEqualTo("0x-wallet");
            assertThat(request.getHolderPkey()).isEqualTo("private-key");
            assertThat(request.getApproved()).isEqualTo("0x-owner");
            assertThat(request.getAmount()).isEqualTo(String.valueOf(Long.MAX_VALUE));
        });
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

        assertThatThrownBy(() -> service.approveActivatedWallet(7L, "0x-wallet", "private-key"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("DaeguChain reward reclaim approval failed")
                .hasMessageContaining("approval operation failed")
                .hasMessageContaining("P06D999");
    }

    @Test
    void retriesApprovalWhileActivatedAccountIsPropagating() {
        when(token20Service.approveToken(any()))
                .thenThrow(new BadRequestApiException("P06D502 Account not found: sender account"))
                .thenThrow(new BadRequestApiException("P06D502 Account not found: sender account"))
                .thenReturn(new DaeguChainDto.ApiResponse<>("OK", Map.of(), "", null, "cid-approve"));

        service.approveActivatedWallet(7L, "0x-wallet", "private-key");

        verify(token20Service, times(4)).approveToken(any());
    }
}
