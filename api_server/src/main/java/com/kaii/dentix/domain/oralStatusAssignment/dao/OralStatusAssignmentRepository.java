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

    interface QuestionnaireOralStatusProjection {
        Long getQuestionnaireId();
        String getOralStatusType();
    }

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_oral_status (questionnaire_id, oral_status_type, created) VALUES (:questionnaireId, :oralStatusType, :created)", nativeQuery = true)
    int nativeInsert(Long questionnaireId, String oralStatusType, Date created);

    @Query(value = "SELECT questionnaire_id AS questionnaireId, oral_status_type AS oralStatusType " +
            "FROM user_oral_status WHERE questionnaire_id IN (:questionnaireIds)", nativeQuery = true)
    List<QuestionnaireOralStatusProjection> findQuestionnaireOralStatuses(List<Long> questionnaireIds);

    @Query(value = "SELECT oral_status_type FROM user_oral_status WHERE questionnaire_id = :questionnaireId", nativeQuery = true)
    List<String> findOralStatusTypesByQuestionnaireId(Long questionnaireId);

    @Query(value = "SELECT oral_status_type FROM user_oral_status WHERE oral_check_id = :oralCheckId", nativeQuery = true)
    List<String> findOralStatusTypesByOralCheckId(Long oralCheckId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_oral_status WHERE questionnaire_id IN (:questionnaireIds)", nativeQuery = true)
    void deleteByQuestionnaireIds(List<Long> questionnaireIds);
}
