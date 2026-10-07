package com.kaii.dentix.domain.admin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.AdminRewardTransferRecoveryDto;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.*;
import com.kaii.dentix.domain.reward.domain.*;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.security.AdminAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminRewardTransferRecoveryServiceTest {
    UserRewardTransactionRepository rewards=mock(UserRewardTransactionRepository.class);
    UserRewardWalletRepository wallets=mock(UserRewardWalletRepository.class);
    UserRepository users=mock(UserRepository.class);
    RewardTransferRecoveryEvidenceRepository evidence=mock(RewardTransferRecoveryEvidenceRepository.class);
    DaeguChainClient chain=mock(DaeguChainClient.class);
    AdminAccessGuard guard=mock(AdminAccessGuard.class);
    PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    DaeguChainProperties properties=new DaeguChainProperties();
    ObjectMapper json=new ObjectMapper();
    String hash="a".repeat(64);
    UserRewardTransaction reward;
    UserRewardWallet wallet;
    AdminRewardTransferRecoveryService service;

    @BeforeEach void setup() throws Exception {
        properties.setTokenOwnerAddress("owner");properties.setToken("test-token");
        reward=UserRewardTransaction.builder().userRewardTransactionId(10L).userId(1L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN).status(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW)
                .amount(1).coinId("optional_video_1").tokenContractAddress("contract")
                .transferRecipientAddress("wallet").transferAttempts(1)
                .transferStartedAt(Date.from(Instant.parse("2026-10-06T00:00:00Z"))).build();
        wallet=UserRewardWallet.builder().userId(1L).walletAddress("wallet").pointBalance(0).build();
        when(rewards.findTransferUserId(10L)).thenReturn(Optional.of(1L));
        when(rewards.findByIdForUpdate(10L)).thenReturn(Optional.of(reward));
        when(wallets.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(User.builder().userId(1L).userLoginIdentifier("test-user").build()));
        when(guard.currentAdmin()).thenReturn(Admin.builder().adminId(9L).adminIsSuper(YnType.Y).build());
        when(manager.getTransaction(any())).thenAnswer(i->new SimpleTransactionStatus());
        service=new AdminRewardTransferRecoveryService(rewards,wallets,users,evidence,chain,properties,guard,manager);
        proof("contract","wallet","1",true);
    }
    AdminRewardTransferRecoveryDto.Request request(){return new AdminRewardTransferRecoveryDto.Request(hash);}
    void proof(String contract,String receiver,String amount,boolean received) throws Exception {
        var data=json.readTree("""
                {"height":100,"in_state":%s,"operation":{"fact":{"hash":"%s","sender":"owner",
                 "receiver":"%s","contract":"%s","amount":"%s"}}}
                """.formatted(received,hash,receiver,contract,amount));
        when(chain.getTransaction(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK",null,"",data,null));
        block("2026-10-07T00:00:00Z");
    }
    void block(String time) throws Exception {
        when(chain.getBlockByNumber(any())).thenReturn(new DaeguChainDto.ApiResponse<>("OK",null,"",
                json.readTree("{\"block\":{\"Manifest\":{\"height\":100,\"proposed_at\":\""+time+"\"}}}"),null));
    }
    @Test void previewQueriesProofWithoutChangingStatusOrPoints() {
        var p=service.preview(10L,request());
        assertThat(p.factHash()).isEqualTo(hash);assertThat(p.height()).isEqualTo(100);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(wallet.getPointBalance()).isZero();verify(evidence,never()).saveAndFlush(any());
    }
    @Test void confirmedProofCreditsOnceAndRepeatedConfirmationIsIdempotent() {
        service.confirm(10L,request());service.confirm(10L,request());
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFERRED);
        assertThat(reward.getDaeguChainFactHash()).isEqualTo(hash);
        assertThat(reward.getTransferRecoveredAt()).isNotNull();assertThat(wallet.getPointBalance()).isEqualTo(1);
        verify(evidence,times(1)).saveAndFlush(any());verify(chain,times(1)).getTransaction(any());
        verify(chain,never()).transferToken(any());verify(chain,never()).createToken(any());
    }
    @Test void nonSuperAdminCannotEvenLookupProof() {
        doThrow(new com.kaii.dentix.global.common.error.exception.UnauthorizedException()).when(guard).requireSuperAdmin();
        assertThatThrownBy(()->service.preview(10L,request())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(chain,evidence,rewards,users,wallets);
    }
    @Test void wrongContractOrRecipientOrAmountDoesNotCredit() throws Exception {
        proof("other-contract","wallet","1",true);
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        proof("contract","other-wallet","1",true);
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        proof("contract","wallet","2",true);
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(wallet.getPointBalance()).isZero();verify(evidence,never()).saveAndFlush(any());
    }
    @Test void rejectedOrUnavailableProofDoesNotCredit() throws Exception {
        proof("contract","wallet","1",false);
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        when(chain.getTransaction(any())).thenThrow(new RuntimeException("test timeout"));
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(wallet.getPointBalance()).isZero();
    }
    @Test void proofOlderThanSubmissionIsRejected() throws Exception {
        block("2026-10-05T00:00:00Z");
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(wallet.getPointBalance()).isZero();
    }
    @Test void previouslyClaimedProofCannotBeAssignedToDifferentReward() {
        when(rewards.findByDaeguChainFactHash(hash)).thenReturn(List.of(UserRewardTransaction.builder().userRewardTransactionId(99L).build()));
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(wallet.getPointBalance()).isZero();verify(evidence,never()).saveAndFlush(any());
    }
    @Test void walletChangeDuringLookupRejectsConfirmation() {
        when(chain.getTransaction(any())).thenAnswer(invocation->{
            wallet.updateDaeguWallet(null,"changed-wallet");
            return new DaeguChainDto.ApiResponse<>("OK",null,"",json.readTree("""
                    {"height":100,"in_state":true,"operation":{"fact":{"hash":"%s","sender":"owner",
                    "receiver":"wallet","contract":"contract","amount":"1"}}}
                    """.formatted(hash)),null);
        });
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(wallet.getPointBalance()).isZero();verify(evidence,never()).saveAndFlush(any());
    }
    @Test void malformedHashAndNoSubmissionCheckpointAreRejectedBeforeLookup() {
        assertThatThrownBy(()->service.preview(10L,new AdminRewardTransferRecoveryDto.Request("invalid"))).isInstanceOf(RuntimeException.class);
        reward=UserRewardTransaction.builder().userRewardTransactionId(10L).userId(1L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN).status(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW)
                .amount(1).transferRecipientAddress("wallet").tokenContractAddress("contract").build();
        when(rewards.findByIdForUpdate(10L)).thenReturn(Optional.of(reward));
        assertThatThrownBy(()->service.preview(10L,request())).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(chain);
    }
    @Test void providerErrorDoesNotExposeItsMessage() {
        when(chain.getTransaction(any())).thenThrow(new com.kaii.dentix.global.common.error.exception.BadRequestApiException("sensitive-provider-payload"));
        assertThatThrownBy(()->service.confirm(10L,request())).hasMessageNotContaining("sensitive-provider-payload");
        assertThat(wallet.getPointBalance()).isZero();
    }
    @Test void revokedSuperPrivilegeDuringLookupPreventsCredit() {
        doNothing().doThrow(new com.kaii.dentix.global.common.error.exception.UnauthorizedException())
                .when(guard).requireSuperAdmin();
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(wallet.getPointBalance()).isZero();verify(evidence,never()).saveAndFlush(any());
    }
    @Test void concurrentEvidenceClaimCannotCredit() {
        when(evidence.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(()->service.confirm(10L,request())).isInstanceOf(RuntimeException.class);
        assertThat(wallet.getPointBalance()).isZero();
        assertThat(reward.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
    }
    @Test void idempotentResponseIsBuiltInsideTransactionForLazyContent() {
        var transactionOpen=new java.util.concurrent.atomic.AtomicBoolean(false);
        when(manager.getTransaction(any())).thenAnswer(i->{transactionOpen.set(true);return new SimpleTransactionStatus();});
        doAnswer(i->{transactionOpen.set(false);return null;}).when(manager).commit(any());
        var content=mock(com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent.class);
        when(content.getTitle()).thenAnswer(i->{assertThat(transactionOpen.get()).isTrue();return "Intro";});
        org.springframework.test.util.ReflectionTestUtils.setField(reward,"oralExerciseContent",content);
        reward.markTokenTransferred(null,hash);
        assertThat(service.confirm(10L,request()).getContentTitle()).isEqualTo("Intro");
        assertThat(wallet.getPointBalance()).isZero();verifyNoInteractions(chain);
    }
    @Test void adminRecoveryDuringWorkerWaitCannotConsumeSameReceiptTwice() throws Exception {
        var pending=UserRewardTransaction.builder().userRewardTransactionId(20L).userId(1L)
                .type(UserRewardTransactionType.ORAL_EXERCISE_COIN).status(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING)
                .amount(1).coinId("optional_video_1").tokenContractAddress("contract").transferRecipientAddress("wallet").build();
        when(rewards.findTransferUserId(20L)).thenReturn(Optional.of(1L));
        when(rewards.findByIdForUpdate(20L)).thenReturn(Optional.of(pending));
        var claims=new java.util.HashMap<String,RewardTransferRecoveryEvidence>();
        when(evidence.findById(anyString())).thenAnswer(i->Optional.ofNullable(claims.get(i.getArgument(0))));
        when(evidence.saveAndFlush(any())).thenAnswer(i->{var claim=(RewardTransferRecoveryEvidence)i.getArgument(0);
            if(claims.putIfAbsent(claim.getFactHash(),claim)!=null)throw new org.springframework.dao.DataIntegrityViolationException("duplicate");
            return claim;});
        var token=mock(com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient.class);
        var worker=new com.kaii.dentix.domain.reward.application.RewardTransferRecoveryService(rewards,wallets,users,token,chain,properties,
                mock(com.kaii.dentix.domain.daeguChain.application.DaeguRewardWalletProvisioningService.class),
                mock(com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService.class),manager,evidence);
        when(token.transferTokenToWallet(anyString(),anyString(),anyString(),anyLong())).thenAnswer(i->{
            service.confirm(10L,request());
            return json.readTree("""
                    {"fact_hash":"%s","data":{"receipt":{"height":100,"in_state":true,"operation":{"fact":{
                    "hash":"%s","sender":"owner","receiver":"wallet","contract":"contract","amount":"1"}}}}}
                    """.formatted(hash,hash));
        });
        try {worker.process(20L);} finally {worker.shutdown();}
        assertThat(wallet.getPointBalance()).isEqualTo(1);
        assertThat(pending.getStatus()).isEqualTo(UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW);
        assertThat(claims.get(hash).getTransactionId()).isEqualTo(10L);
    }
}
