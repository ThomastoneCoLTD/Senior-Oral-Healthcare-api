package com.kaii.dentix.domain.gingivitis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.gingivitis.domain.GingivitisResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

public class GingivitisDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResponse {
        private Long analysisId;
        private String state;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long analysisId;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        private Date createdAt;
        private GingivitisResultType resultType;
        private String resultLabel;
        private Float totalPercent;
        private Integer upperPercent;
        private Integer lowerPercent;
        private String comment;
        private List<ContentsDto.Summary> contents;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionResponse {
        private Long latestAnalysisId;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        private Date latestDate;
        private GingivitisResultType latestResultType;
        private Float latestPercent;
        private List<ContentsDto.Summary> contents;
        private List<History> history;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class History {
        private Long analysisId;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private Date createdAt;
        private GingivitisResultType resultType;
        private Float totalPercent;
    }
}
