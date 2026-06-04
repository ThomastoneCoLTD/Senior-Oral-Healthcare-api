package com.kaii.dentix.domain.reward.dao;

import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserRewardWalletRepository extends JpaRepository<UserRewardWallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserRewardWallet> findByUserId(Long userId);
}
