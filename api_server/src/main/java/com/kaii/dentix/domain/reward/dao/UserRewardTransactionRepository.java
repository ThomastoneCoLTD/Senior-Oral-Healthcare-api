package com.kaii.dentix.domain.reward.dao;

import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionStatus;
import com.kaii.dentix.domain.reward.domain.UserRewardTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<UserRewardTransaction> findByUserIdOrderByCreatedDesc(Long userId);
}
