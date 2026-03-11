package com.kaii.dentix.domain.userOralStatus.domain;

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
@Table(name = "user_oral_status") // 이건 기존대로 유지
public class UserOralStatus extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userOralStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaireId")
    private Questionnaire questionnaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oralCheckId")
    private OralCheck oralCheck;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "oralStatusType", nullable = true)
    private OralStatus oralStatus;

    public static UserOralStatus forQuestionnaire(Questionnaire questionnaire, String oralStatusType) {
        return new UserOralStatus(questionnaire, new OralStatus(oralStatusType));
    }

    public static UserOralStatus forOralCheck(OralCheck oralCheck, String oralStatusType) {
        return new UserOralStatus(oralCheck, new OralStatus(oralStatusType));
    }

    private UserOralStatus(Questionnaire questionnaire, OralStatus oralStatus) {
        this.questionnaire = questionnaire;
        this.oralStatus = oralStatus;
    }

    private UserOralStatus(OralCheck oralCheck, OralStatus oralStatus) {
        this.oralCheck = oralCheck;
        this.oralStatus = oralStatus;
    }
}
