package com.kaii.dentix.domain.questionnaire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
//import com.kaii.dentix.domain.type.OralStatusType; // 만약 Enum이 있다면 사용, 없으면 String
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.oralStatus.dto.OralStatusDto;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class QuestionnaireDto {

    // =================================================================
    // 1. 문진표 양식 (Template) - 다국어 Map 타입 유지
    // =================================================================
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateJson {
        private String version;
        private List<Template> template;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Template {
        private int sort;
        private String key;
        private String number;

        // 기존 타입 유지: 다국어 지원을 위한 Map
        private Map<String, String> title;
        private Map<String, String> description;

        private Integer minimum;
        private Integer maximum;

        private List<TemplateContent> contents;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TemplateContent {
        private int id;
        // 기존 타입 유지: 다국어 지원을 위한 Map
        private Map<String, String> text;
    }

    // =================================================================
    // 2. 문진표 제출 요청 (SubmitRequest) - Integer[] 배열 타입 유지
    // =================================================================
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        @NotNull(message = "문진표 작성은 필수입니다.")
        private List<Answer> form;
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Answer {
        private String key;
        // 기존 타입 유지: 배열(Array) 사용
        private Integer[] value;
    }

    // =================================================================
    // 3. 문진표 ID 응답 (IdResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class IdResponse {
        private Long questionnaireId;
    }

    // =================================================================
    // 4. 문진표 결과 상세 조회 (ResultResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ResultResponse {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private Date created;

        private List<OralStatusDto.Info> oralStatusList;

        // 리팩토링된 ContentsDto.Summary 사용
        private List<ContentsDto.Summary> contents;
    }



    // =================================================================
    // 6. AI 분석 결과 (AnalysisResponse)
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class AnalysisResponse {
        @Builder.Default
        @JsonProperty("status_code")
        private int statusCode = 200;

        @JsonProperty("status_msg")
        private String statusMsg;

        @JsonProperty("contents_type")
        private List<String> contentsType;
    }

    // =================================================================
    // 7. 문진표 요약 정보 (Summary) - 목록 조회용
    // =================================================================
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private Long questionnaireId;
        private Date created;
        private String oralStatusType;
        private String oralStatusTitle;
    }
}