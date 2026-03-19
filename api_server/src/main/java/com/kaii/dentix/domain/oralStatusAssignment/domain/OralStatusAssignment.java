package com.kaii.dentix.domain.oralStatusAssignment.domain;

import com.kaii.dentix.domain.oralCheck.domain.OralCheck;
import jakarta.persistence.*;
import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.global.common.entity.TimeEntity;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// DB migration 전까지는 기존 테이블명을 유지한다.
@Table(name = "user_oral_status")
public class OralStatusAssignment extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_oral_status_id")
    private Long oralStatusAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaire_id")
    private Questionnaire questionnaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oral_check_id")
    private OralCheck oralCheck;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "oral_status_type", nullable = false)
    private OralStatus oralStatus;

    public static OralStatusAssignment forQuestionnaire(Questionnaire questionnaire, String oralStatusType) {
        return new OralStatusAssignment(questionnaire, new OralStatus(oralStatusType));
    }

    public static OralStatusAssignment forOralCheck(OralCheck oralCheck, String oralStatusType) {
        return new OralStatusAssignment(oralCheck, new OralStatus(oralStatusType));
    }

    private OralStatusAssignment(Questionnaire questionnaire, OralStatus oralStatus) {
        this.questionnaire = questionnaire;
        this.oralStatus = oralStatus;
    }

    private OralStatusAssignment(OralCheck oralCheck, OralStatus oralStatus) {
        this.oralCheck = oralCheck;
        this.oralStatus = oralStatus;
    }
}
