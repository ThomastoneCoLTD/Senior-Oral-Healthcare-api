package com.kaii.dentix.domain.passwordReset.dao;

import com.kaii.dentix.domain.passwordReset.domain.PasswordResetAccountType;
import com.kaii.dentix.domain.passwordReset.domain.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHashAndAccountType(
            String tokenHash,
            PasswordResetAccountType accountType
    );

    @Modifying
    @Query("delete from PasswordResetToken token where token.accountType = :accountType and token.accountId = :accountId")
    void deleteByAccount(
            @Param("accountType") PasswordResetAccountType accountType,
            @Param("accountId") Long accountId
    );
}
