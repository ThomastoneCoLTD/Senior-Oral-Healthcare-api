package com.kaii.dentix.domain.reward.domain;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_reward_transaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_reward_transaction_idempotency", columnNames = "idempotency_key")
        }
)
public class UserRewardTransaction extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRewardTransactionId;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oral_exercise_content_id")
    private OralExerciseContent oralExerciseContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRewardTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRewardTransactionStatus status;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(length = 100)
    private String sessionId;

    @Column(length = 100)
    private String coinId;

    @Column(length = 255)
    private String tokenContractAddress;

    @Column(length = 255)
    private String daeguChainTxHash;

    @Column(length = 255)
    private String daeguChainFactHash;

    private Integer transferAttempts;
    private Integer confirmationAttempts;
    private Date nextTransferCheckAt;
    private Date transferStartedAt;
    private Date transferRecoveredAt;
    @Column(length = 255)
    private String transferRecipientAddress;
    @Column(length = 40)
    private String transferFailureCode;

    public int transferAttempts() { return transferAttempts == null ? 0 : transferAttempts; }
    public int confirmationAttempts() { return confirmationAttempts == null ? 0 : confirmationAttempts; }

    public boolean isTransferUnresolved() {
        return type == UserRewardTransactionType.ORAL_EXERCISE_COIN &&
                (status == UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING
                || status == UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING
                || status == UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW
                || status == UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED && transferFailureCode == null);
    }

    public UserRewardTransactionStatus displayStatus() {
        return status == UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED && isTransferUnresolved()
                ? UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW : status;
    }

    public void queueTransfer(String recipient, String contract) {
        transferRecipientAddress = recipient;
        tokenContractAddress = contract;
        status = UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING;
        nextTransferCheckAt = new Date();
        transferFailureCode = null;
    }

    // This state must be committed before making a request that can move tokens.
    public void claimTransfer(Date now) {
        // Never use a previously rejected fact as evidence about a new submission.
        daeguChainTxHash = null;
        daeguChainFactHash = null;
        status = UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING;
        transferAttempts = transferAttempts() + 1;
        confirmationAttempts = 0;
        transferStartedAt = now;
        nextTransferCheckAt = new Date(now.getTime() + 90_000L);
        transferFailureCode = "AWAITING_RESPONSE";
    }

    public void claimConfirmation(Date now) {
        confirmationAttempts = confirmationAttempts() + 1;
        nextTransferCheckAt = new Date(now.getTime() + 90_000L);
    }

    public void awaitConfirmation(String hash, String code) {
        status = UserRewardTransactionStatus.TOKEN_TRANSFER_CHECKING;
        if (hash != null) daeguChainFactHash = hash;
        transferFailureCode = code;
        nextTransferCheckAt = new Date(System.currentTimeMillis() + 30_000L);
    }

    public void retryNotSubmitted() {
        transferFailureCode = "NOT_SUBMITTED";
        status = transferAttempts() >= 3 ? UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED
                : UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING;
        nextTransferCheckAt = status == UserRewardTransactionStatus.TOKEN_TRANSFER_PENDING
                ? new Date(System.currentTimeMillis() + 5_000L * (1L << Math.min(transferAttempts(), 3))) : null;
    }

    public void requireTransferReview(String code) {
        status = UserRewardTransactionStatus.TOKEN_TRANSFER_REVIEW;
        transferFailureCode = code;
        nextTransferCheckAt = null;
    }

    public void markTransferRejected() {
        status = UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED;
        transferFailureCode = "CHAIN_REJECTED";
        nextTransferCheckAt = null;
    }

    public void markTransferRecovered(String hash) {
        markTokenTransferred(hash, daeguChainFactHash);
        transferRecoveredAt = new Date();
    }

    public void markPointMinted(String txHash, String factHash) {
        this.status = UserRewardTransactionStatus.POINT_MINTED;
        this.daeguChainTxHash = txHash;
        this.daeguChainFactHash = factHash;
    }

    public void markPointMintFailed() {
        this.status = UserRewardTransactionStatus.POINT_MINT_FAILED;
    }

    public void markTokenTransferred(String txHash, String factHash) {
        this.status = UserRewardTransactionStatus.TOKEN_TRANSFERRED;
        this.daeguChainTxHash = txHash;
        this.daeguChainFactHash = factHash;
        this.nextTransferCheckAt = null;
        this.transferFailureCode = null;
        if (transferAttempts() > 1) this.transferRecoveredAt = new Date();
    }

    public void updateTokenContractAddress(String tokenContractAddress) {
        this.tokenContractAddress = tokenContractAddress;
    }

    public void markTokenTransferFailed() {
        this.status = UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED;
    }

    public void updateBalanceAfter(long balanceAfter) {
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("balanceAfter must not be negative");
        }
        this.balanceAfter = balanceAfter;
    }

    public boolean isAlreadyApplied() {
        return status != UserRewardTransactionStatus.CANCELED;
    }

    public boolean isRewardReceived() {
        return status == UserRewardTransactionStatus.LOCAL_RECORDED
                || status == UserRewardTransactionStatus.TOKEN_TRANSFERRED
                || status == UserRewardTransactionStatus.POINT_MINTED;
    }
}
