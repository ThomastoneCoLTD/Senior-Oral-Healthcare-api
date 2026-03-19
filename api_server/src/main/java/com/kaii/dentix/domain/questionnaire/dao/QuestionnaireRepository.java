package com.kaii.dentix.domain.questionnaire.dao;

import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {

    List<Questionnaire> findAllByUserIdOrderByCreatedDesc(Long userId);

    Optional<Questionnaire> findTopByUserIdOrderByCreatedDesc(Long userId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE questionnaire SET created = :created WHERE questionnaire_id = :questionnaireId", nativeQuery = true)
    int nativeUpdate(Long questionnaireId, Date created);
}
