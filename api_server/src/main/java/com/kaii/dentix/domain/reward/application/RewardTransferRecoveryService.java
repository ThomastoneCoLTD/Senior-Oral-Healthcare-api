package com.kaii.dentix.domain.reward.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiLogContext;
import com.kaii.dentix.domain.daeguChain.application.DaeguRewardWalletProvisioningService;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.client.ExternalTokenClient;
import com.kaii.dentix.domain.daeguChain.client.TokenTransferException;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.oralExercise.application.OralExerciseHistoryService;
import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionEventType;
import com.kaii.dentix.domain.reward.dao.*;
import com.kaii.dentix.domain.reward.domain.*;
import com.kaii.dentix.domain.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import jakarta.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardTransferRecoveryService {
    private final UserRewardTransactionRepository rewards;
    private final UserRewardWalletRepository wallets;
    private final UserRepository users;
    private final ExternalTokenClient tokenClient;
    private final DaeguChainClient chainClient;
    private final DaeguChainProperties properties;
    private final DaeguRewardWalletProvisioningService provisioning;
    private final OralExerciseHistoryService history;
    private final PlatformTransactionManager transactionManager;
    private final RewardTransferRecoveryEvidenceRepository evidence;
    // Keep upstream waits off the shared Spring scheduler and bound per-instance load.
    private final Semaphore capacity = new Semaphore(4);
    private final ExecutorService workers = Executors.newFixedThreadPool(4, task -> {
        Thread thread = new Thread(task, "reward-transfer-worker");
        thread.setDaemon(true);
        return thread;
    });

    @Scheduled(fixedDelayString = "${user-reward.recovery-poll-ms:1000}")
    public void recoverDueTransfers() {
        int available = capacity.availablePermits();
        if (available == 0 || workers.isShutdown()) return;
        for (Long id : rewards.findDueTransfers(List.of(UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING,
                UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING), new Date(), PageRequest.of(0, available))) {
            if (!capacity.tryAcquire()) break;
            try {
                workers.execute(() -> {
                    try { process(id); }
                    catch (RuntimeException e) {
                        // Checkpoints stay durable. Never log upstream bodies or keys.
                        log.warn("Reward recovery deferred. transactionId={}, errorType={}", id, e.getClass().getSimpleName());
                    } finally { capacity.release(); }
                });
            } catch (java.util.concurrent.RejectedExecutionException e) {
                capacity.release();
                break;
            }
        }
    }

    @PreDestroy
    public void shutdown() { workers.shutdown(); }

    public void process(Long id) {
        Work work = tx().execute(ignored -> claim(id));
        if (work == null) return;
        if (!work.send()) { confirm(work); return; }
        try {
            JsonNode response = DaeguChainApiLogContext.withUser(work.userId(), "구강체조 리워드 지급",
                    () -> tokenClient.transferTokenToWallet(work.coinId().toUpperCase(java.util.Locale.ROOT),
                            work.contract(), work.recipient(), work.amount()));
            String factHash = text(response, "fact_hash");
            var result = RewardTransferReceipt.verify(response.path("data"), factHash,
                    properties.getTokenOwnerAddress(), work.recipient(), work.contract(), work.amount());
            if (result == RewardTransferReceipt.Result.RECEIVED) {
                finishReceived(work, text(response, "tx_hash"), factHash, receiptHeight(response.path("data")), false);
            } else if (result == RewardTransferReceipt.Result.REJECTED) {
                retryRejected(work);
                recordFailure(work);
            } else {
                update(work, reward -> reward.awaitConfirmation(factHash, "UPSTREAM_UNCONFIRMED"));
            }
        } catch (TokenTransferException e) {
            update(work, reward -> {
                if (e.isNotSubmitted()) reward.retryNotSubmitted();
                else reward.awaitConfirmation(e.getFactHash(), "UPSTREAM_UNCONFIRMED");
            });
            recordFailure(work);
        } catch (RuntimeException e) {
            // Includes uncertain response processing. No exception-message inference or blind retry.
            update(work, reward -> reward.awaitConfirmation(null, "UPSTREAM_UNCONFIRMED"));
            recordFailure(work);
        }
    }

    private Work claim(Long id) {
        var userId = rewards.findTransferUserId(id).orElse(null);
        if (userId == null || users.findByIdForUpdate(userId).isEmpty()) return null;
        var wallet = wallets.findByUserIdForUpdate(userId).orElse(null);
        var reward = rewards.findByIdForUpdate(id).orElse(null);
        if (reward == null || !reward.isTransferUnresolved() || reward.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW) return null;
        Date now = new Date();
        if (reward.getNextTransferCheckAt() != null && reward.getNextTransferCheckAt().after(now)) return null;
        if (wallet == null || reward.getTransferRecipientAddress() == null
                || !reward.getTransferRecipientAddress().equals(wallet.getWalletAddress()) || reward.getTokenContractAddress() == null) {
            reward.requireTransferReview("WALLET_VERIFICATION_REQUIRED");
            return null;
        }
        boolean send = reward.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING;
        if (send) reward.claimTransfer(now);
        else reward.claimConfirmation(now);
        return new Work(id, reward.getUserId(), reward.getCoinId(), reward.getTokenContractAddress(),
                reward.getTransferRecipientAddress(), reward.getAmount(), reward.getDaeguChainFactHash(),
                reward.transferAttempts(), send, reward.getSessionId(),
                reward.getOralExerciseContent() == null ? null : reward.getOralExerciseContent().getOralExerciseContentId(),
                wallet.getWalletPrivateKeyCiphertext());
    }

    private void confirm(Work work) {
        try {
            // Useful diagnostic evidence, but never interpret zero/positive balance as a receipt.
            DaeguChainApiLogContext.withUser(work.userId(), "리워드 지급 결과 잔액 확인",
                    () -> chainClient.getTokenBalance(DaeguChainDto.TokenBalanceApiRequest.builder()
                            .token(properties.resolveUserToken()).chain(properties.getChain())
                            .contAddr(work.contract()).addr(work.recipient()).build()));
            if (work.factHash() == null || !work.factHash().matches("[A-Za-z0-9]{32,100}")) {
                update(work, reward -> reward.requireTransferReview("RECEIPT_REQUIRED"));
                return;
            }
            var response = DaeguChainApiLogContext.withUser(work.userId(), "리워드 지급 거래 확인",
                    () -> chainClient.getTransaction(DaeguChainDto.TransactionApiRequest.builder()
                            .token(properties.resolveUserToken()).chain(properties.getChain()).factHash(work.factHash()).build()));
            var result = response != null && "OK".equalsIgnoreCase(response.getState())
                    ? RewardTransferReceipt.verify(response.getData(), work.factHash(), properties.getTokenOwnerAddress(),
                        work.recipient(), work.contract(), work.amount()) : RewardTransferReceipt.Result.UNKNOWN;
            if (result == RewardTransferReceipt.Result.RECEIVED) finishReceived(work, null, work.factHash(), receiptHeight(response.getData()), true);
            else if (result == RewardTransferReceipt.Result.REJECTED) {
                retryRejected(work);
            } else postpone(work);
        } catch (RuntimeException e) { postpone(work); }
    }

    private void retryRejected(Work work) {
        update(work, reward -> {
            if (reward.transferAttempts() < 3) reward.queueTransfer(work.recipient(), work.contract());
            else reward.markTransferRejected();
        });
    }

    private void postpone(Work work) {
        update(work, reward -> {
            if (reward.confirmationAttempts() >= 6) reward.requireTransferReview("RECEIPT_REQUIRED");
            else reward.awaitConfirmation(work.factHash(), "UPSTREAM_UNCONFIRMED");
        });
    }

    private void finishReceived(Work work, String hash, String factHash, long height, boolean recovered) {
        try { tx().execute(ignored -> {
            if (users.findByIdForUpdate(work.userId()).isEmpty()) return false;
            var wallet = wallets.findByUserIdForUpdate(work.userId()).orElse(null);
            var reward = rewards.findByIdForUpdate(work.id()).orElse(null);
            if (wallet == null || reward == null || reward.getStatus() != UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING
                    || reward.transferAttempts() != work.attempt() || !work.recipient().equals(wallet.getWalletAddress())) return false;
            if (evidence.findById(factHash).isPresent() || rewards.findByDaeguChainFactHash(factHash).stream()
                    .anyMatch(other -> !work.id().equals(other.getUserRewardTransactionId()))) {
                reward.requireTransferReview("RECEIPT_ALREADY_CLAIMED");
                return false;
            }
            // Same INSERT-only claim as manual recovery: one chain receipt can credit one reward.
            evidence.saveAndFlush(RewardTransferRecoveryEvidence.builder().factHash(factHash).transactionId(work.id())
                    .recoveredAt(new Date()).blockHeight(height).build());
            reward.markTokenTransferred(hash, factHash);
            if (recovered) reward.markTransferRecovered(hash);
            wallet.addPoints(reward.getAmount());
            reward.updateBalanceAfter(wallet.getPointBalance());
            return true;
        }); } catch (org.springframework.dao.DataIntegrityViolationException conflict) {
            // Unique-key failure rolls back before points/status change; never retry the transfer.
            update(work, reward -> reward.requireTransferReview("RECEIPT_ALREADY_CLAIMED"));
        }
        // Approval is performed only when an administrator actually reclaims the exact amount.

    }

    private void update(Work work, Consumer<UserRewardTransaction> change) {
        tx().executeWithoutResult(ignored -> {
            if (users.findByIdForUpdate(work.userId()).isEmpty()) return;
            wallets.findByUserIdForUpdate(work.userId());
            rewards.findByIdForUpdate(work.id()).filter(reward -> reward.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING
                    && reward.transferAttempts() == work.attempt()).ifPresent(change);
        });
    }

    private void recordFailure(Work work) {
        if (work.contentId() == null || work.sessionId() == null) return;
        try { history.recordFailure(work.userId(), new OralExerciseHistoryService.FailureRequest(
                work.contentId(), work.sessionId(), OralExerciseInteractionEventType.TOKEN_FAILED), true); }
        catch (RuntimeException e) { log.warn("Reward failure history deferred. transactionId={}", work.id()); }
    }

    private TransactionTemplate tx() {
        var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        String value = node.path(field).asText(null);
        if (value == null) value = node.path("data").path(field).asText(null);
        if (value == null) value = node.path("data").path("tx").path(field.equals("tx_hash") ? "hash" : field).asText(null);
        if (value == null && field.equals("fact_hash"))
            value = node.path("data").path("receipt").path("operation").path("fact").path("hash").asText(null);
        return value != null && value.matches("[A-Za-z0-9]{32,100}") ? value : null;
    }

    private static long receiptHeight(JsonNode data) {
        // Called only after RewardTransferReceipt has validated the receipt.
        if (data.has("receipt")) data = data.path("receipt");
        else if (data.has("trx_info")) data = data.path("trx_info");
        return data.path("height").asLong();
    }

    private record Work(Long id, Long userId, String coinId, String contract, String recipient, long amount,
                        String factHash, int attempt, boolean send, String sessionId, Long contentId, String keyCiphertext) {}
}
