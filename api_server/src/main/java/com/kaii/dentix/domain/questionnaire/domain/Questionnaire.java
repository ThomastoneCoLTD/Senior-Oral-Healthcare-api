package com.kaii.dentix.domain.questionnaire.domain;

import com.kaii.dentix.domain.oralStatus.domain.OralStatus;
import com.kaii.dentix.domain.userOralStatus.domain.UserOralStatus;
import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter @Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "questionnaire")
public class Questionnaire extends TimeEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionnaireId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String questionnaireVersion;

    @Column(name = "form", columnDefinition = "json")
    private String form;

    @Builder.Default
    @OneToMany(mappedBy = "questionnaire", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<UserOralStatus> userOralStatusList = new ArrayList<>();

    /**
     * 기존: oralStatusTypeList를 함께 저장하는 생성자
     */
    public Questionnaire(Long userId, String questionnaireVersion, String form, List<String> oralStatusTypeList) {
        this.userId = userId;
        this.questionnaireVersion = questionnaireVersion;
        this.form = form;
        assignOralStatuses(oralStatusTypeList);
    }

    public void assignOralStatuses(List<String> oralStatusTypeList) {
        this.userOralStatusList = oralStatusTypeList.stream()
                .map(oralStatusType -> UserOralStatus.forQuestionnaire(this, oralStatusType))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<OralStatus> getOralStatuses() {
        return userOralStatusList.stream()
                .map(UserOralStatus::getOralStatus)
                .toList();
    }
}
