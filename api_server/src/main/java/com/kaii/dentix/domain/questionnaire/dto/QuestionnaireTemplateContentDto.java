package com.kaii.dentix.domain.questionnaire.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class QuestionnaireTemplateContentDto {

    private int sort;
    private int id;

    // ✅ 문자열 → 다국어 Map으로 변경
    private Map<String, String> text;
}
