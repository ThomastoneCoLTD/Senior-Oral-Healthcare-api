package com.kaii.dentix.domain.reward.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;
import java.util.Date;

/** Immutable receipt ownership for automatic/admin completion. Retained after reset or deletion. */
@Entity
@Table(name = "reward_transfer_recovery_evidence")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardTransferRecoveryEvidence implements Persistable<String> {
    @Id
    @Column(name = "fact_hash", length = 100, nullable = false, updatable = false)
    private String factHash;
    @Column(nullable = false, updatable = false)
    private Long transactionId;
    // Null identifies automatic receipt confirmation; manual recovery records the super administrator.
    @Column(updatable = false)
    private Long adminId;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date recoveredAt;
    @Column(nullable = false, updatable = false)
    private long blockHeight;

    @Override public String getId() { return factHash; }
    // Always INSERT: merge could overwrite another transaction's receipt claim under concurrency.
    @Override @Transient public boolean isNew() { return true; }
}
