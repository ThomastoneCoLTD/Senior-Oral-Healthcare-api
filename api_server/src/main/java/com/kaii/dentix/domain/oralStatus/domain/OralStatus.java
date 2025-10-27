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
@Table(name = "oralStatus")
public class OralStatus extends TimeEntity {

    @Id
    private String oralStatusType;

    @Column(nullable = false)
    private String oralStatusTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oralStatusDescription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oralStatusSubDescription;

    @Column(nullable = false)
    private int oralStatusPriority;

    // ✅ 다국어 컬럼 추가
    @Column
    private String oralStatusTitleEn;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionEn;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionEn;

    @Column
    private String oralStatusTitleVi;

    @Column(columnDefinition = "TEXT")
    private String oralStatusDescriptionVi;

    @Column(columnDefinition = "TEXT")
    private String oralStatusSubDescriptionVi;

    public OralStatus(String oralStatusType) {
        this.oralStatusType = oralStatusType;
    }
}