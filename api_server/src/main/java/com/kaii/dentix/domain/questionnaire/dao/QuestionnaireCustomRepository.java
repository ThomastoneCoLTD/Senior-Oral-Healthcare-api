package com.kaii.dentix.domain.questionnaire.dao;

import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
import com.kaii.dentix.domain.admin.dto.statistic.QuestionnaireStatisticDto;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireDto;

import java.util.List;

public interface QuestionnaireCustomRepository {
    QuestionnaireDto.Summary getLatestQuestionnaireAndHigherStatus(Long userId);
    List<QuestionnaireStatisticDto> questionnaireList(AdminStatisticDto.SearchRequest request);
    int allQuestionnaireCount(AdminStatisticDto.SearchRequest request);
}

