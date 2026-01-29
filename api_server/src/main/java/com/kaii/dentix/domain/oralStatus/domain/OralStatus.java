package com.kaii.dentix.domain.oralStatus.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter @Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oral_status")
public class OralStatus extends TimeEntity {

    @Id
    private String oralStatusType;

    @Column(nullable = false)
    private int oralStatusPriority;

    //다국어 컬럼 추가
    // --- 한국어 (Default) ---
    @Column(nullable = false)
    private String oralStatusTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oralStatusDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oralStatusSubDescription;

    // --- 영어 (En) ---
    @Column
    private String oralStatusTitleEn;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionEn;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionEn;

    // --- 베트남어 (Vi) ---
    @Column
    private String oralStatusTitleVi;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionVi;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionVi;

    // --- [신규] 중국어 간체 (zh-CN) ---
    @Column
    private String oralStatusTitleZhCn;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionZhCn;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionZhCn;

    // --- [신규] 중국어 번체 (zh-TW) ---
    @Column
    private String oralStatusTitleZhTw;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionZhTw;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionZhTw;

    // --- [신규] 일본어 (ja) ---
    @Column
    private String oralStatusTitleJa;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionJa;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionJa;

    public OralStatus(String oralStatusType) {
        this.oralStatusType = oralStatusType;
    }
}