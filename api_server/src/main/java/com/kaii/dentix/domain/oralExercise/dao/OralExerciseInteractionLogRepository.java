package com.kaii.dentix.domain.oralExercise.dao;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseInteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OralExerciseInteractionLogRepository extends JpaRepository<OralExerciseInteractionLog, Long> {
}
