package com.kaii.dentix.domain.oralExercise.dao;

import com.kaii.dentix.domain.oralExercise.domain.UserOralExerciseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserOralExerciseProgressRepository extends JpaRepository<UserOralExerciseProgress, Long> {

    List<UserOralExerciseProgress> findByUserId(Long userId);

    List<UserOralExerciseProgress> findByUserIdIn(List<Long> userIds);

    Optional<UserOralExerciseProgress> findByUserIdAndContent_OralExerciseContentId(
            Long userId,
            Long oralExerciseContentId
    );
}
