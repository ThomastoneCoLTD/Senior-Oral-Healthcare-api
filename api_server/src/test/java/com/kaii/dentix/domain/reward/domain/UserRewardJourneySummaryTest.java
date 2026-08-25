package com.kaii.dentix.domain.reward.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserRewardJourneySummaryTest {

    @Test
    void completesAfterAllReceivedTokensAreReclaimed() {
        List<UserRewardTransaction> transactions = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            transactions.add(transaction(
                    UserRewardTransactionType.ORAL_EXERCISE_COIN,
                    UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                    "essential_video_" + index
            ));
            transactions.add(transaction(
                    UserRewardTransactionType.ORAL_EXERCISE_RECLAIM,
                    UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                    "essential_video_" + index
            ));
        }

        UserRewardJourneySummary summary = UserRewardJourneySummary.from(transactions);

        assertThat(summary.state()).isEqualTo(UserRewardJourneyState.COMPLETED);
        assertThat(summary.essentialReceivedCount()).isEqualTo(5);
        assertThat(summary.essentialReclaimedCount()).isEqualTo(5);
        assertThat(summary.pendingReclaimCount()).isZero();
        assertThat(summary.canReclaim()).isFalse();
        assertThat(summary.completed()).isTrue();
    }

    @Test
    void remainsPartiallyReclaimedWhenAnOptionalTokenIsStillHeld() {
        List<UserRewardTransaction> transactions = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            transactions.add(transaction(
                    UserRewardTransactionType.ORAL_EXERCISE_COIN,
                    UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                    "essential_video_" + index
            ));
            transactions.add(transaction(
                    UserRewardTransactionType.ORAL_EXERCISE_RECLAIM,
                    UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                    "essential_video_" + index
            ));
        }
        transactions.add(transaction(
                UserRewardTransactionType.ORAL_EXERCISE_COIN,
                UserRewardTransactionStatus.TOKEN_TRANSFERRED,
                "optional_video_2"
        ));

        UserRewardJourneySummary summary = UserRewardJourneySummary.from(transactions);

        assertThat(summary.state()).isEqualTo(UserRewardJourneyState.PARTIAL_RECLAIM);
        assertThat(summary.essentialReceivedCount()).isEqualTo(5);
        assertThat(summary.essentialReclaimedCount()).isEqualTo(5);
        assertThat(summary.pendingReclaimCount()).isEqualTo(1);
        assertThat(summary.canReclaim()).isTrue();
        assertThat(summary.completed()).isFalse();
    }

    @Test
    void failedTokenTransfersDoNotCountAsReceived() {
        UserRewardJourneySummary summary = UserRewardJourneySummary.from(List.of(
                transaction(
                        UserRewardTransactionType.ORAL_EXERCISE_COIN,
                        UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED,
                        "essential_video_1"
                )
        ));

        assertThat(summary.state()).isEqualTo(UserRewardJourneyState.COLLECTING);
        assertThat(summary.essentialReceivedCount()).isZero();
        assertThat(summary.pendingReclaimCount()).isZero();
    }

    private UserRewardTransaction transaction(
            UserRewardTransactionType type,
            UserRewardTransactionStatus status,
            String coinId
    ) {
        return UserRewardTransaction.builder()
                .userId(7L)
                .type(type)
                .status(status)
                .amount(1L)
                .balanceAfter(0L)
                .idempotencyKey(type + ":" + coinId)
                .coinId(coinId)
                .build();
    }
}
