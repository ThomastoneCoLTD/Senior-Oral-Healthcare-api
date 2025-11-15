package com.kaii.dentix.domain.questionnaire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.contents.dto.ContentsCategoryDto;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class QuestionnaireResultDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date created;

    private List<OralStatusTypeInfoDto> oralStatusList;

//    private List<ContentsCategoryDto> categories;

    private List<ContentsDto> contents;
}
