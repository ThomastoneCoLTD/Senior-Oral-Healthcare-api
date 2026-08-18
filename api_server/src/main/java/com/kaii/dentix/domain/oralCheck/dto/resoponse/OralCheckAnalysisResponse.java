package com.kaii.dentix.domain.oralCheck.dto.resoponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckAnalysisDivisionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *  구강 검진 사진 분석 결과
 */
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class OralCheckAnalysisResponse {

    @JsonProperty("status_code")
    private int statusCode; // 결과 코드

    @JsonProperty("status_msg")
    private String statusMsg; // 결과 메시지

    @JsonProperty("plaque_stats")
    private OralCheckAnalysisDivisionDto plaqueStats; // 4등분 목록

    @JsonProperty("contents_type")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> contentsType;

    @JsonProperty("plaque_contents")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Long> plaqueContents;

    public OralCheckAnalysisResponse(OralCheckAnalysisDivisionDto plaqueStats) {
        this.statusCode = 200;
        this.plaqueStats = plaqueStats;
    }
}
