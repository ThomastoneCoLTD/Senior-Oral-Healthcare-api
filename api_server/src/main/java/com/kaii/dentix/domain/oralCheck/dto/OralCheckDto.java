package com.kaii.dentix.domain.oralCheck.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaii.dentix.domain.oralStatus.dto.OralStatusDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDto;
import com.kaii.dentix.domain.type.OralDateStatusType;
import com.kaii.dentix.domain.type.OralSectionType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OralCheckDto {

    // =================================================================
    // 1. 촬영/업로드 응답 (PhotoResponse)
    // =================================================================
    @Getter @Setter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class PhotoResponse {
        private Long organizationId;
        private boolean success;
        private Integer remainingResponses;
        private Long oralCheckId;
    }

    // =================================================================
    // 2. 검진 결과 상세 (ResultResponse)
    // =================================================================
    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ResultResponse {
        private Long userId;
        private Long organizationId;
        private boolean success;
        private OralCheckResultType oralCheckResultTotalType;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date created;

        private Float oralCheckTotalRange;
        private Float oralCheckUpRightRange;
        private OralCheckResultType oralCheckUpRightScoreType;
        private Float oralCheckUpLeftRange;
        private OralCheckResultType oralCheckUpLeftScoreType;
        private Float oralCheckDownLeftRange;
        private OralCheckResultType oralCheckDownLeftScoreType;
        private Float oralCheckDownRightRange;
        private OralCheckResultType oralCheckDownRightScoreType;
        private List<String> oralCheckCommentList;
        private Integer remainingResponses;
    }

    // =================================================================
    // 3. 대시보드 (DashboardResponse)
    // =================================================================
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class DashboardResponse {
        private Long latestOralCheckId;
        private Long oralCheckTimeInterval;
        private int oralCheckTotalCount;
        private int oralCheckHealthyCount;
        private int oralCheckGoodCount;
        private int oralCheckAttentionCount;
        private int oralCheckDangerCount;
        private long toothBrushingTotalCount;
        private float toothBrushingAverage;
        private OralStatusDto.OralStatusType oralStatus;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date questionnaireCreated;

        private OralCheckResultType oralCheckResultTotalType;
        private OralCheckResultType oralCheckUpRightScoreType;
        private OralCheckResultType oralCheckUpLeftScoreType;
        private OralCheckResultType oralCheckDownLeftScoreType;
        private OralCheckResultType oralCheckDownRightScoreType;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date oralCheckCreated;

        @Builder.Default
        private List<DailyChange> oralCheckDailyList = new ArrayList<>();
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class DailyChange {
        private int oralCheckNumber;
        private OralCheckResultType oralCheckResultTotalType;
    }

    // =================================================================
    // 4. 타임라인 (TimelineResponse) - 기존 OralCheckDto 대체
    // =================================================================
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class TimelineResponse {
        private List<Section> sectionList;
        private List<Daily> dailyList;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Section {
        @Setter private int sort;
        private OralSectionType sectionType;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
        private Long timeInterval;
        @Builder.Default
        private List<ToothBrushingDto> toothBrushingList = new ArrayList<>();
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Daily {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
        private Date date;
        private OralDateStatusType status;
        private boolean questionnaire;
        @Builder.Default
        private List<Detail> detailList = new ArrayList<>();
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Detail {
        private OralSectionType sectionType;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
        private Date date;
        private long identifier;
        private Long oralCheckId;
        private OralCheckResultType oralCheckResultTotalType;
        private Integer toothBrushingCount;
        @Builder.Default
        private List<OralStatusDto.OralStatusType> oralStatusList = new ArrayList<>();
    }

    // =================================================================
    // 5. AI 분석 결과 (AnalysisResponse)
    // =================================================================
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class AnalysisResponse {
        @Builder.Default @JsonProperty("status_code") private int statusCode = 200;
        @JsonProperty("status_msg") private String statusMsg;
        @JsonProperty("plaque_stats") private AnalysisDivision plaqueStats;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class AnalysisDivision {
        private Float topRight;
        private Float topLeft;
        private Float btmRight;
        private Float btmLeft;
    }
    // =================================================================
    // 6. 기관 사용자 사용량 조회 (UsageResponse)
    // =================================================================
    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class UsageResponse {
        private long totalCount;
        private List<Usage> users;
    }

    @Getter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class Usage {
        private Long userId;
        private String userName;
        private Long successCount;
    }

}
