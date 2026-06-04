package com.kaii.dentix.domain.reward.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_reward_wallet",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_reward_wallet_user", columnNames = "user_id")
        }
)
public class UserRewardWallet extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRewardWalletId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long pointBalance;

    @Column(length = 255)
    private String daeguDid;

    @Column(length = 255)
    private String walletAddress;

    public void addPoints(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.pointBalance += amount;
    }
}
