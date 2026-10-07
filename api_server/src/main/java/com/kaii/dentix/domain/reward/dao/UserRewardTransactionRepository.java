package com.kaii.dentix.domain.reward.dao;

import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Date;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRewardTransactionRepository extends JpaRepository<UserRewardTransaction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from UserRewardTransaction t where t.userRewardTransactionId = :id")
    Optional<UserRewardTransaction> findByIdForUpdate(Long id);

    @Query("select t.userId from UserRewardTransaction t where t.userRewardTransactionId = :id")
    Optional<Long> findTransferUserId(Long id);

    @Query("""
            select t.userRewardTransactionId from UserRewardTransaction t
            where t.type = com.kaii.dentix.domain.reward.domain.UserRewardTransactionType.ORAL_EXERCISE_COIN
            and t.status in :statuses and (t.nextTransferCheckAt is null or t.nextTransferCheckAt <= :now)
            order by t.userRewardTransactionId
            """)
    List<Long> findDueTransfers(Collection<UserRewardTransactionStatus> statuses, Date now, Pageable pageable);

    Optional<UserRewardTransaction> findByIdempotencyKey(String idempotencyKey);

    List<UserRewardTransaction> findByDaeguChainFactHash(String factHash);

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

    long deleteByUserId(Long userId);

    List<UserRewardTransaction> findByUserIdInOrderByCreatedDesc(Collection<Long> userIds);

    List<UserRewardTransaction> findByUserIdInAndType(
            Collection<Long> userIds,
            UserRewardTransactionType type
    );

    List<UserRewardTransaction> findByUserIdInAndTypeOrderByCreatedDesc(
            Collection<Long> userIds,
            UserRewardTransactionType type
    );

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

}
