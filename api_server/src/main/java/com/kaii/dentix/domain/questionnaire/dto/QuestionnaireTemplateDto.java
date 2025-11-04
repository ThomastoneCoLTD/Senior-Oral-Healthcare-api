package com.kaii.dentix.domain.questionnaire.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class QuestionnaireTemplateDto {

  private int sort;
    private String key;
    private String number;

    // ✅ 다국어 대응: 문자열 대신 Map으로 변경
    private Map<String, String> title;
    private Map<String, String> description;

    private Integer minimum;
    private Integer maximum;

    private List<QuestionnaireTemplateContentDto> contents;
}
