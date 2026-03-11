package com.kaii.dentix.domain.oralStatusAssignment.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.kaii.dentix.domain.oralStatusAssignment.domain.OralStatusAssignment;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;

public interface OralStatusAssignmentRepository extends JpaRepository<OralStatusAssignment, Long> {

    List<OralStatusAssignment> findAllByQuestionnaire(Questionnaire questionnaire);
    List<OralStatusAssignment> findAllByQuestionnaireIn(List<Questionnaire> questionnaireList);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_oral_status (questionnaireId, oralStatusType, created) VALUES (:questionnaireId, :oralStatusType, :created)", nativeQuery = true)
    int nativeInsert(Long questionnaireId, String oralStatusType, Date created);
}
