package com.kaii.dentix.domain.user.dao;

import com.kaii.dentix.domain.user.domain.DadaeguSignupSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DadaeguSignupSessionRepository extends JpaRepository<DadaeguSignupSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DadaeguSignupSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from DadaeguSignupSession session where session.externalDid = :externalDid")
    void deleteByExternalDid(@Param("externalDid") String externalDid);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
