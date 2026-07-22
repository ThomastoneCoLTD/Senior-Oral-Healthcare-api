package com.kaii.dentix.domain.user.dao;

import com.kaii.dentix.domain.user.domain.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {

    List<UserLoginHistory> findByUserIdInOrderByCreatedDesc(Collection<Long> userIds);
}
