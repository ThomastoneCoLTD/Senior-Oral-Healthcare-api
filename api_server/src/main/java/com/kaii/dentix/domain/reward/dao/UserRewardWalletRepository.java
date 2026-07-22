package com.kaii.dentix.domain.reward.dao;

import com.kaii.dentix.domain.reward.domain.UserRewardWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRewardWalletRepository extends JpaRepository<UserRewardWallet, Long> {

    Optional<UserRewardWallet> findByUserId(Long userId);

    List<UserRewardWallet> findByUserIdIn(Collection<Long> userIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from UserRewardWallet wallet where wallet.userId = :userId")
    Optional<UserRewardWallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
