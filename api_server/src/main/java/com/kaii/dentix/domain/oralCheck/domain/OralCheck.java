package com.kaii.dentix.domain.oralCheck.domain;

import com.kaii.dentix.domain.type.oral.OralCheckAnalysisState;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.domain.User;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

@Entity
@Getter @Builder
@Setter
@AllArgsConstructor @NoArgsConstructor
@Where(clause = "oral_check_analysis_state = 'SUCCESS'")
@Table(name = "oral_check")
public class OralCheck extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oralCheckId;

    @Column(name = "subscription_history_id")
    private Long subscriptionHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
//    private Long userId;

    @Column(length = 200, nullable = false)
    private String oralCheckPicturePath; // 구강 검진 원본 사진 경로

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum", name = "oral_check_analysis_state")
    private OralCheckAnalysisState oralCheckAnalysisState; // 구강 검진 분석 상태

    private Float oralCheckTotalRange; // 구강 검진 전체 비율

    private Float oralCheckUpRightRange; // 구강 검진 우상 비율

    private Float oralCheckUpLeftRange; // 구강 검진 좌상 비율

    private Float oralCheckDownRightRange; // 구강 검진 우하 비율

    private Float oralCheckDownLeftRange; // 구강 검진 좌하 비율

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String oralCheckResultJsonData; // 결과 JSON data 전체

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private OralCheckResultType oralCheckResultTotalType; // 종합 결과 유형

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private OralCheckResultType oralCheckUpRightScoreType; // 우상 점수 유형

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private OralCheckResultType oralCheckUpLeftScoreType; // 좌상 점수 유형

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private OralCheckResultType oralCheckDownRightScoreType; // 우하 점수 유형

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "enum")
    private OralCheckResultType oralCheckDownLeftScoreType; // 좌하 점수 유형

}
