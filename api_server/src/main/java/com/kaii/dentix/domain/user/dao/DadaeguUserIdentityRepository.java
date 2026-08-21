package com.kaii.dentix.domain.user.dao;

import com.kaii.dentix.domain.user.domain.DadaeguUserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DadaeguUserIdentityRepository extends JpaRepository<DadaeguUserIdentity, Long> {
    Optional<DadaeguUserIdentity> findByExternalDid(String externalDid);
    Optional<DadaeguUserIdentity> findByCiHash(String ciHash);
    Optional<DadaeguUserIdentity> findByUserId(Long userId);
}
