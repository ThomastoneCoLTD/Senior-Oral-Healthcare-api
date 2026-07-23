package com.kaii.dentix.domain.daeguChain.dao;

import com.kaii.dentix.domain.daeguChain.domain.DaeguChainApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DaeguChainApiLogRepository extends JpaRepository<DaeguChainApiLog, Long> {

    List<DaeguChainApiLog> findByUserIdInOrderByCreatedDesc(Collection<Long> userIds);
}
