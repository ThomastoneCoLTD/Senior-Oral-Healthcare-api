package com.kaii.dentix.domain.oralCheck.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaii.dentix.domain.questionnaire.dto.OralStatusTypeDto;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
public class DashboardDto {

    private Long latestOralCheckId;

    private Long oralCheckTimeInterval;

    private int oralCheckTotalCount;
    private int oralCheckHealthyCount;
    private int oralCheckGoodCount;
    private int oralCheckAttentionCount;
    private int oralCheckDangerCount;

    private long toothBrushingTotalCount;
    private float toothBrushingAverage;

    private OralStatusTypeDto oralStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date questionnaireCreated;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
//    private Date oralCheck
    private OralCheckResultType oralCheckResultTotalType; // 전체 구강 상태 결과 타입
    private OralCheckResultType oralCheckUpRightScoreType; // 우상 점수 유형
    private OralCheckResultType oralCheckUpLeftScoreType; // 좌상 점수 유형
    private OralCheckResultType oralCheckDownLeftScoreType; // 좌하 점수 유형
    private OralCheckResultType oralCheckDownRightScoreType; // 우하 점수 유형
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date oralCheckCreated;

    @Builder.Default
    private List<OralCheckDailyChangeDto> oralCheckDailyList = new ArrayList<>();
}
