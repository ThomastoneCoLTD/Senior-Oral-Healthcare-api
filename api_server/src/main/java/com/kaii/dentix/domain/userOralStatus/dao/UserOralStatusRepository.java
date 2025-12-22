package com.kaii.dentix.domain.userOralStatus.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus;

public interface UserOralStatusRepository extends JpaRepository<UserOralStatus, Long> {

    List<UserOralStatus> findAllByQuestionnaire(Questionnaire questionnaire);
    List<UserOralStatus> findAllByQuestionnaireIn(List<Questionnaire> questionnaireList);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO userOralStatus (questionnaireId, oralStatusType, created) VALUES (:questionnaireId, :oralStatusType, :created)", nativeQuery = true)
    int nativeInsert(Long questionnaireId, String oralStatusType, Date created);
}
