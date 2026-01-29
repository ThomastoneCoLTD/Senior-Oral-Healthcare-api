package com.kaii.dentix.domain.questionnaire.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *  문진표 분석 결과
 */
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class QuestionnaireAnalysisResponse {

    @JsonProperty("status_code")
    private int statusCode; // 결과 코드

    @JsonProperty("status_msg")
    private String statusMsg; // 결과 메시지

    @JsonProperty("contents_type")
    private List<String> contentsType; // 문진표 컨텐츠 AI 모델 결과 값

    public QuestionnaireAnalysisResponse(List<String> contentsType) {
        this.statusCode = 200;
        this.contentsType = contentsType;
    }
}
