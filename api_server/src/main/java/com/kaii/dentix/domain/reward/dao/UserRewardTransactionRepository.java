package com.kaii.dentix.domain.reward.dao;

import com.kaii.dentix.domain.reward.domain.UserRewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRewardTransactionRepository extends JpaRepository<UserRewardTransaction, Long> {

    Optional<UserRewardTransaction> findByIdempotencyKey(String idempotencyKey);

    List<UserRewardTransaction> findByUserIdOrderByCreatedDesc(Long userId);
}
