package com.kaii.dentix.domain.userOralStatus.domain;

import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.questionnaire.domain.Questionnaire;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_oral_status")
public class UserOralStatus extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userOralStatusId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "questionnaireId", nullable = false)
    private Questionnaire questionnaire;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "oralStatusType")
    private OralStatus oralStatus;

    public UserOralStatus(Questionnaire questionnaire, OralStatus oralStatus) {
        this.questionnaire = questionnaire;
        this.oralStatus = oralStatus;
    }
}
