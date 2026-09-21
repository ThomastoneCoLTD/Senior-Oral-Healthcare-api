package com.kaii.dentix.domain.intakeSurvey;

import com.kaii.dentix.domain.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface IntakeSurveyRepository extends JpaRepository<UserIntakeSurvey, Long> {
    // Serialize even the first save, when no survey row exists yet.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    User lockUser(@Param("userId") Long userId);
}
