package com.kaii.dentix.domain.reward.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record UserRewardJourneySummary(
        UserRewardJourneyState state,
        int essentialReceivedCount,
        int essentialReclaimedCount,
        int pendingReclaimCount,
        boolean canReclaim,
        boolean completed
) {

    private static final int ESSENTIAL_REWARD_COUNT = 5;
    private static final Set<String> ESSENTIAL_TOKEN_NAMES = IntStream
            .rangeClosed(1, ESSENTIAL_REWARD_COUNT)
            .mapToObj(index -> "essential_video_" + index)
            .collect(Collectors.toUnmodifiableSet());
    private static final EnumSet<UserRewardTransactionStatus> NON_REWARDED_STATUSES = EnumSet.of(
            UserRewardTransactionStatus.CANCELED,
            UserRewardTransactionStatus.TOKEN_TRANSFER_FAILED,
            UserRewardTransactionStatus.POINT_MINT_FAILED
    );
    private static final EnumSet<UserRewardTransactionStatus> COMPLETED_RECLAIM_STATUSES = EnumSet.of(
            UserRewardTransactionStatus.LOCAL_RECORDED,
            UserRewardTransactionStatus.POINT_MINTED,
            UserRewardTransactionStatus.TOKEN_TRANSFERRED
    );

    public static UserRewardJourneySummary from(List<UserRewardTransaction> transactions) {
        List<UserRewardTransaction> safeTransactions = transactions == null ? List.of() : transactions;
        Set<String> receivedCoinIds = safeTransactions.stream()
                .filter(transaction -> transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_COIN)
                .filter(transaction -> !NON_REWARDED_STATUSES.contains(transaction.getStatus()))
                .map(UserRewardTransaction::getCoinId)
                .filter(UserRewardJourneySummary::hasText)
                .map(UserRewardJourneySummary::normalize)
                .collect(Collectors.toSet());
        Set<String> reclaimedCoinIds = safeTransactions.stream()
                .filter(transaction -> transaction.getType() == UserRewardTransactionType.ORAL_EXERCISE_RECLAIM)
                .filter(transaction -> COMPLETED_RECLAIM_STATUSES.contains(transaction.getStatus()))
                .map(UserRewardTransaction::getCoinId)
                .filter(UserRewardJourneySummary::hasText)
                .map(UserRewardJourneySummary::normalize)
                .collect(Collectors.toSet());

        int essentialReceivedCount = (int) ESSENTIAL_TOKEN_NAMES.stream()
                .filter(receivedCoinIds::contains)
                .count();
        int essentialReclaimedCount = (int) ESSENTIAL_TOKEN_NAMES.stream()
                .filter(reclaimedCoinIds::contains)
                .count();
        int pendingReclaimCount = (int) receivedCoinIds.stream()
                .filter(coinId -> !reclaimedCoinIds.contains(coinId))
                .count();
        boolean essentialCollectionCompleted = essentialReceivedCount == ESSENTIAL_REWARD_COUNT;
        boolean completed = essentialCollectionCompleted
                && essentialReclaimedCount == ESSENTIAL_REWARD_COUNT
                && pendingReclaimCount == 0;
        boolean canReclaim = essentialCollectionCompleted && pendingReclaimCount > 0;

        UserRewardJourneyState state;
        if (completed) {
            state = UserRewardJourneyState.COMPLETED;
        } else if (!reclaimedCoinIds.isEmpty() && pendingReclaimCount > 0) {
            state = UserRewardJourneyState.PARTIAL_RECLAIM;
        } else if (canReclaim) {
            state = UserRewardJourneyState.READY;
        } else {
            state = UserRewardJourneyState.COLLECTING;
        }

        return new UserRewardJourneySummary(
                state,
                essentialReceivedCount,
                essentialReclaimedCount,
                pendingReclaimCount,
                canReclaim,
                completed
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
