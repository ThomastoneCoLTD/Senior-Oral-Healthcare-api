package com.kaii.dentix.domain.questionnaire.dao;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.statistic.QuestionnaireStatisticDto;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireAndStatusDto;

import java.util.List;

public interface QuestionnaireCustomRepository {

    QuestionnaireAndStatusDto getLatestQuestionnaireAndHigherStatus(Long userId);

    List<QuestionnaireStatisticDto> questionnaireList(AdminStatisticDto.SearchRequest request);

    int allQuestionnaireCount(AdminStatisticDto.SearchRequest request);

}

