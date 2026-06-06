package com.kaii.dentix.domain.reward.domain;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

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
    private String daeguChainTxHash;

    @Column(length = 255)
    private String daeguChainFactHash;

    public void markPointMinted(String txHash, String factHash) {
        this.status = UserRewardTransactionStatus.POINT_MINTED;
        this.daeguChainTxHash = txHash;
        this.daeguChainFactHash = factHash;
    }

    public void markPointMintFailed() {
        this.status = UserRewardTransactionStatus.POINT_MINT_FAILED;
    }

    public boolean isAlreadyApplied() {
        return status != UserRewardTransactionStatus.CANCELED;
    }
}
