package com.kaii.dentix.domain.oralExercise.dao;

import com.kaii.dentix.domain.oralExercise.domain.OralExerciseContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OralExerciseContentRepository extends JpaRepository<OralExerciseContent, Long> {

    List<OralExerciseContent> findByActiveTrueOrderByContentSortAsc();

    Optional<OralExerciseContent> findByContentSort(int contentSort);
}
