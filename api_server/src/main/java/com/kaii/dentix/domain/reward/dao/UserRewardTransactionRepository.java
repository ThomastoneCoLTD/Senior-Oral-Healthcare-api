package com.kaii.dentix.domain.reward.dao;

import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRewardTransactionRepository extends JpaRepository<UserRewardTransaction, Long> {

    Optional<UserRewardTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<UserRewardTransaction> findFirstByUserIdAndOralExerciseContent_OralExerciseContentIdAndTypeAndStatusNot(
            Long userId,
            Long oralExerciseContentId,
            UserRewardTransactionType type,
            UserRewardTransactionStatus status
    );

    Optional<UserRewardTransaction> findFirstByUserIdAndCoinIdAndTypeAndStatusNot(
            Long userId,
            String coinId,
            UserRewardTransactionType type,
            UserRewardTransactionStatus status
    );

    List<UserRewardTransaction> findByUserIdOrderByCreatedDesc(Long userId);

    @Query("""
            select rewardTransaction
            from UserRewardTransaction rewardTransaction
            left join fetch rewardTransaction.oralExerciseContent
            where rewardTransaction.type = :type
            order by rewardTransaction.created desc
            """)
    List<UserRewardTransaction> findRecentByType(
            @Param("type") UserRewardTransactionType type,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(rewardTransaction.amount), 0)
            from UserRewardTransaction rewardTransaction
            where rewardTransaction.userId = :userId
              and rewardTransaction.type = :type
              and rewardTransaction.status not in :excludedStatuses
            """)
    long sumRewardedAmount(
            @Param("userId") Long userId,
            @Param("type") UserRewardTransactionType type,
            @Param("excludedStatuses") Collection<UserRewardTransactionStatus> excludedStatuses
    );
}
