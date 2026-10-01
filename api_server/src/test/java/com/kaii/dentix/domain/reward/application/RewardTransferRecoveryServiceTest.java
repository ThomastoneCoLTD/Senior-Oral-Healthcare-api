package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.daeguChain.application.DaeguRewardWalletProvisioningService;
import com.kaii.dentix.domain.daeguChain.client.*;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService;
import com.kaii.dentix.domain.reward.dao.*;
import com.kaii.dentix.domain.reward.domain.*;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.Date;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class RewardTransferRecoveryServiceTest {
    UserRewardTransactionRepository rewards = mock(UserRewardTransactionRepository.class);
    UserRewardWalletRepository wallets = mock(UserRewardWalletRepository.class);
    UserRepository users = mock(UserRepository.class);
    ExternalTokenClient token = mock(ExternalTokenClient.class);
    DaeguChainClient chain = mock(DaeguChainClient.class);
    DaeguRewardWalletProvisioningService provisioning = mock(DaeguRewardWalletProvisioningService.class);
    OralExerciseHistoryService history = mock(OralExerciseHistoryService.class);
    PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    DaeguChainProperties properties = new DaeguChainProperties();
    ObjectMapper mapper = new ObjectMapper();
    UserRewardTransaction reward;
    UserRewardWallet wallet;
    RewardTransferRecoveryService service;
    String hash = "a".repeat(64);

    @BeforeEach void setup() {
        properties.setTokenOwnerAddress("owner");
        properties.setToken("test-only");
        reward = UserRewardTransaction.builder().userRewardTransactionId(10L).userId(1L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN).status(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING)
                .amount(1).coinId("essential_video_1").tokenContractAddress("contract")
                .transferRecipientAddress("wallet").idempotencyKey("test").build();
        wallet = UserRewardWallet.builder().userId(1L).walletAddress("wallet").pointBalance(0).build();
        when(rewards.findTransferUserId(10L)).thenReturn(Optional.of(1L));
        when(rewards.findByIdForUpdate(10L)).thenReturn(Optional.of(reward));
        when(wallets.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().userId(1L).build()));
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        service = new RewardTransferRecoveryService(rewards, wallets, users, token, chain, properties, provisioning, history, manager);
    }

    void due() { org.springframework.test.util.ReflectionTestUtils.setField(reward, "nextTransferCheckAt", new Date(0)); }

    @Test void checkpointIsCommittedBeforeTransferAndRepeatedProcessingDoesNotCreditTwice() throws Exception {
        when(token.transferTokenToWallet(anyString(),anyString(),anyString(),anyLong())).thenAnswer(i -> {
            assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING);
            verify(manager).commit(any());
            return mapper.readTree("""
                {"state":"OK","data":{"tx":{"fact_hash":"%s","hash":"%s"},
                "receipt":{"height":100,"in_state":true,"operation":{"fact":{"hash":"%s","sender":"owner",
                "receiver":"wallet","contract":"contract","amount":"1"}}}}}
                """.formatted(hash,hash,hash));
        });
        service.process(10L); service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFERRED);
        assertThat(wallet.getPointBalance()).isEqualTo(1);
        verify(token,times(1)).transferTokenToWallet(anyString(),anyString(),anyString(),anyLong());
    }

    @Test void onlyConnectionFailureBeforeSubmissionIsRetriedAndStopsAtThreeAttempts() {
        when(token.transferTokenToWallet(anyString(),anyString(),anyString(),anyLong())).thenThrow(new TokenTransferException(true,null));
        for(int i=0;i<3;i++){due();service.process(10L);}
        assertThat(reward.transferAttempts()).isEqualTo(3);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED);
        assertThat(wallet.getPointBalance()).isZero();
        service.process(10L);
        verify(token,times(3)).transferTokenToWallet(anyString(),anyString(),anyString(),anyLong());
    }

    @Test void ambiguousTransferWithoutReceiptIsNeverResentEvenWhenBalanceIsZero() throws Exception {
        when(token.transferTokenToWallet(anyString(),anyString(),anyString(),anyLong())).thenThrow(new TokenTransferException(false,null));
        when(chain.getTokenBalance(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK",null,"",mapper.readTree("{\"balance\":0}"),null));
        service.process(10L);due();service.process(10L);service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(wallet.getPointBalance()).isZero();
        verify(token,times(1)).transferTokenToWallet(anyString(),anyString(),anyString(),anyLong());
        verify(chain,never()).getTransaction(any());
    }

    @Test void restartAfterCheckpointOnlyConfirmsAndDoesNotSubmitAgain() {
        reward.claimTransfer(new Date(0));due();
        service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        verifyNoInteractions(token);
    }

    @Test void exactConfirmedReceiptRestoresPointsOnceAndApprovalFailureDoesNotUndoIt() throws Exception {
        reward.claimTransfer(new Date(0));reward.awaitConfirmation(hash,"UPSTREAM_UNCONFIRMED");due();
        when(chain.getTransaction(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK",null,"",mapper.readTree("""
                {"height":100,"in_state":true,"operation":{"fact":{"hash":"%s","sender":"owner",
                 "items":[{"receiver":"wallet","contract":"contract","amount":"1"}]}}}
                """.formatted(hash)),null));
        doThrow(new RuntimeException("approval unavailable")).when(provisioning).approveRewardContract(anyLong(),anyString(),anyString(),any());
        service.process(10L);service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFERRED);
        assertThat(reward.getTransferRecoveredAt()).isNotNull();
        assertThat(wallet.getPointBalance()).isEqualTo(1);
        verifyNoInteractions(token);
    }

    @Test void malformedReceiptOrReceiptForAnotherWalletCannotCreditOrTriggerRetry() throws Exception {
        reward.claimTransfer(new Date(0));reward.awaitConfirmation(hash,"UPSTREAM_UNCONFIRMED");due();
        when(chain.getTransaction(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK",null,"",mapper.readTree("""
                {"height":100,"in_state":true,"operation":{"fact":{"hash":"%s","sender":"owner",
                 "items":[{"receiver":"other-wallet","contract":"contract","amount":"1"}]}}}
                """.formatted(hash)),null));
        for(int i=0;i<6;i++){due();service.process(10L);}
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(wallet.getPointBalance()).isZero();
        verifyNoInteractions(token);
    }

    @Test void changedWalletOrDeletedUserPreventsDispatch() {
        wallet.updateDaeguWallet("did", "changed-wallet");service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        verifyNoInteractions(token);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.empty());service.process(10L);
        verifyNoInteractions(token);
    }

    @Test void rejectedReceiptQueuesOnlyOneNewSubmissionAndCannotBeReusedForThatSubmission() throws Exception {
        reward.claimTransfer(new Date(0));reward.awaitConfirmation(hash,"UPSTREAM_UNCONFIRMED");due();
        when(chain.getTransaction(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK",null,"",mapper.readTree("""
                {"height":100,"in_state":false,"operation":{"fact":{"hash":"%s","sender":"owner",
                 "items":[{"receiver":"wallet","contract":"contract","amount":"1"}]}}}
                """.formatted(hash)),null));
        service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING);
        when(token.transferTokenToWallet(anyString(),anyString(),anyString(),anyLong()))
                .thenThrow(new TokenTransferException(false,null));
        due();service.process(10L);
        assertThat(reward.getDaeguChainFactHash()).isNull();
        due();service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(wallet.getPointBalance()).isZero();
        verify(chain,times(1)).getTransaction(any());
        verify(token,times(1)).transferTokenToWallet(anyString(),anyString(),anyString(),anyLong());
    }

    @Test void legacyFailureWithoutSubmissionEvidenceIsNeverResent() {
        reward.markTokenTransferFailed();
        org.springframework.test.util.ReflectionTestUtils.setField(reward,"transferRecipientAddress",null);
        service.process(10L);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        verifyNoInteractions(token);
    }
}
