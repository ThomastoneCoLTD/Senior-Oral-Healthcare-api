package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dto.AdminDaeguChainTokenDto;
import com.kaii.dentix.domain.admin.dto.AdminRewardTransferRecoveryDto;
import com.kaii.dentix.domain.daeguChain.client.DaeguChainClient;
import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.domain.daeguChain.application.DaeguChainApiLogContext;
import com.kaii.dentix.domain.daeguChain.dto.DaeguChainDto;
import com.kaii.dentix.domain.reward.dao.*;
import com.kaii.dentix.domain.reward.domain.*;
import com.kaii.dentix.domain.reward.application.RewardTransferReceipt;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import com.kaii.dentix.global.security.AdminAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AdminRewardTransferRecoveryService {
    private final UserRewardTransactionRepository rewards;
    private final UserRewardWalletRepository wallets;
    private final UserRepository users;
    private final RewardTransferRecoveryEvidenceRepository evidence;
    private final DaeguChainClient chain;
    private final DaeguChainProperties properties;
    private final AdminAccessGuard guard;
    private final PlatformTransactionManager manager;

    public AdminRewardTransferRecoveryDto.Preview preview(Long id, AdminRewardTransferRecoveryDto.Request request) {
        guard.requireSuperAdmin();
        String hash = validatedHash(request);
        Target target = inTransaction(() -> locked(id, hash).target());
        if (target.status() == UserRewardTransactionStatus.TOKEN_TRANSFERRED) {
            throw invalid("이미 지급이 완료된 건입니다.");
        }
        ensureUnclaimed(hash, id);
        long height = verifyProof(target, hash);
        return new AdminRewardTransferRecoveryDto.Preview(id, hash, properties.getTokenOwnerAddress(),
                target.recipient(), target.contract(), target.amount(), height, target.status());
    }
    public AdminDaeguChainTokenDto.RewardTransfer confirm(Long id, AdminRewardTransferRecoveryDto.Request request) {
        guard.requireSuperAdmin();
        Long adminId = guard.currentAdmin().getAdminId();
        String hash = validatedHash(request);
        Preparation initial = inTransaction(() -> {
            Snapshot snapshot = locked(id, hash);
            return new Preparation(snapshot.target(), snapshot.reward().getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFERRED
                    ? snapshot.response() : null);
        });
        if (initial.completedResponse() != null) return initial.completedResponse();
        ensureUnclaimed(hash, id);
        // No DB lock is held while waiting on the provider. Confirm always fetches fresh proof.
        long height = verifyProof(initial.target(), hash);
        return inTransaction(() -> {
            guard.requireSuperAdmin();
            Snapshot current = locked(id, hash);
            if (current.reward().getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFERRED) return current.response();
            if (!initial.target().equals(current.target())) throw invalid("지급 정보가 변경되었습니다. 다시 확인해 주세요.");
            ensureUnclaimed(hash, id);
            try {
                evidence.saveAndFlush(RewardTransferRecoveryEvidence.builder().factHash(hash).transactionId(id)
                        .adminId(adminId).recoveredAt(new Date()).blockHeight(height).build());
            } catch (DataIntegrityViolationException conflict) {
                throw invalid("이미 다른 복구에서 사용된 거래 증명입니다.");
            }
            current.reward().markTokenTransferred(null, hash);
            current.reward().markTransferRecovered(null);
            current.wallet().addPoints(current.reward().getAmount());
            current.reward().updateBalanceAfter(current.wallet().getPointBalance());
            return current.response();
        });
    }

    private String validatedHash(AdminRewardTransferRecoveryDto.Request request) {
        String hash = request == null ? null : request.factHash();
        if (hash == null || !hash.matches("[A-Za-z0-9]{32,100}")) throw invalid("거래 fact hash 형식이 올바르지 않습니다.");
        return hash;
    }

    private Snapshot locked(Long id, String hash) {
        Long userId = rewards.findTransferUserId(id).orElseThrow(() -> invalid("지급 기록을 찾을 수 없습니다."));
        // Same lock order as dispatch, recovery and reset: user -> wallet -> reward.
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> invalid("사용자를 찾을 수 없습니다."));
        if (user.getDeleted() != null) throw invalid("삭제된 사용자의 지급은 복구할 수 없습니다.");
        UserRewardWallet wallet = wallets.findByUserIdForUpdate(userId).orElseThrow(() -> invalid("지갑을 찾을 수 없습니다."));
        UserRewardTransaction reward = rewards.findByIdForUpdate(id).orElseThrow(() -> invalid("지급 기록을 찾을 수 없습니다."));
        if (!Objects.equals(userId, reward.getUserId()) || reward.getType() != UserRewardTransactionType.ORAL_EXERCISE_COIN
                || reward.getAmount() <= 0 || reward.getTokenContractAddress() == null
                || reward.getTransferRecipientAddress() == null
                || !reward.getTransferRecipientAddress().equals(wallet.getWalletAddress())) {
            throw invalid("지급 대상 지갑과 기록이 일치하지 않습니다.");
        }
        if (reward.getDaeguChainFactHash() != null && !hash.equals(reward.getDaeguChainFactHash())) {
            throw invalid("이미 기록된 거래 fact hash와 일치하지 않습니다.");
        }
        if (reward.getStatus() != UserRewardTransactionStatus.TOKEN_TRANSFERRED
                && (reward.displayStatus() != UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW
                    && reward.getStatus() != UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING)) {
            throw invalid("관리자 확인이 필요한 지급만 복구할 수 있습니다.");
        }
        if (reward.getStatus() == UserRewardTransactionStatus.TOKEN_TRANSFERRED
                && !hash.equals(reward.getDaeguChainFactHash())) throw invalid("이미 완료된 지급입니다.");
        if (reward.getTransferStartedAt() == null || reward.transferAttempts() < 1) {
            throw invalid("전송 시점 기록이 없어 거래와 연결할 수 없습니다. 운영 담당자 확인이 필요합니다.");
        }
        Target target = new Target(id, userId, reward.getTokenContractAddress(), reward.getTransferRecipientAddress(),
                reward.getAmount(), reward.getTransferStartedAt().getTime(), reward.getStatus(), reward.transferAttempts(),
                properties.getTokenOwnerAddress());
        return new Snapshot(target, reward, wallet, user);
    }

    private void ensureUnclaimed(String hash, Long id) {
        if (evidence.findById(hash).isPresent()
                || rewards.findByDaeguChainFactHash(hash).stream().anyMatch(r -> !id.equals(r.getUserRewardTransactionId()))) {
            throw invalid("이미 다른 지급에 사용된 거래 증명입니다.");
        }
    }

    private long verifyProof(Target target, String hash) {
        try {
            return DaeguChainApiLogContext.withUser(target.userId(), "관리자 지급 거래 증명 확인", () -> {
                var result = chain.getTransaction(DaeguChainDto.TransactionApiRequest.builder()
                        .token(properties.resolveUserToken()).chain(properties.getChain()).factHash(hash).build());
                if (result == null || !"OK".equalsIgnoreCase(result.getState())
                        || RewardTransferReceipt.verify(result.getData(), hash, target.owner(), target.recipient(),
                            target.contract(), target.amount()) != RewardTransferReceipt.Result.RECEIVED) {
                    throw invalid("성공한 거래 증명이 지급 정보와 일치하지 않습니다.");
                }
                var receipt = result.getData();
                if (receipt.has("receipt")) receipt = receipt.path("receipt");
                else if (receipt.has("trx_info")) receipt = receipt.path("trx_info");
                long height = receipt.path("height").asLong();
                var block = chain.getBlockByNumber(DaeguChainDto.BlockByNumberApiRequest.builder()
                        .token(properties.resolveUserToken()).chain(properties.getChain()).blockNum(Long.toString(height)).build());
                if (block == null || !"OK".equalsIgnoreCase(block.getState()) || block.getData() == null) {
                    throw invalid("거래 블록의 처리 시점을 확인할 수 없습니다.");
                }
                var manifest = block.getData().path("block").path("Manifest");
                if (!manifest.path("height").isIntegralNumber() || manifest.path("height").asLong() != height) {
                    throw invalid("거래 블록 정보가 일치하지 않습니다.");
                }
                Instant proposedAt;
                try { proposedAt = Instant.parse(manifest.path("proposed_at").asText()); }
                catch (DateTimeParseException e) { throw invalid("거래 블록 시각을 확인할 수 없습니다."); }
                // Allow 5 min clock skew, never use an old successful payment as this request's proof.
                if (proposedAt.toEpochMilli() < target.startedAt() - 300_000L) {
                    throw invalid("전송 요청 이전의 거래는 복구 증명으로 사용할 수 없습니다.");
                }
                return height;
            });
        } catch (EvidenceValidationException e) { throw e; }
        catch (RuntimeException e) { throw invalid("대구체인 거래 증명을 조회하지 못했습니다. 지급 상태는 변경하지 않았습니다."); }
    }

    private <T> T inTransaction(Supplier<T> action) {
        return new TransactionTemplate(manager).execute(ignored -> action.get());
    }
    private EvidenceValidationException invalid(String message) { return new EvidenceValidationException(message); }
    private static class EvidenceValidationException extends BadRequestApiException {
        EvidenceValidationException(String message) { super(message); }
    }
    private record Target(Long id, Long userId, String contract, String recipient, long amount,
                          long startedAt, UserRewardTransactionStatus status, int attempts, String owner) {}
    private record Preparation(Target target, AdminDaeguChainTokenDto.RewardTransfer completedResponse) {}
    private record Snapshot(Target target, UserRewardTransaction reward, UserRewardWallet wallet, User user) {
        AdminDaeguChainTokenDto.RewardTransfer response() { return AdminDaeguChainTokenDto.RewardTransfer.from(reward, user); }
    }
}
