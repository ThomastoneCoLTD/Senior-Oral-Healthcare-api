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

    @OneToMany(mappedBy = "questionnaire", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<UserOralStatus> userOralStatusList = new ArrayList<>();

    /**
     * ✅ 기존: oralStatusTypeList를 함께 저장하는 생성자
     */
    public Questionnaire(Long userId, String questionnaireVersion, String form, List<String> oralStatusTypeList) {
        this.userId = userId;
        this.questionnaireVersion = questionnaireVersion;
        this.form = form;
        this.userOralStatusList = oralStatusTypeList.stream()
                .map(oralStatusType -> new UserOralStatus(this, new OralStatus(oralStatusType)))
                .collect(Collectors.toList());
    }

    /**
     * ✅ 추가: oralStatusTypeList 없이 기본 생성 (이번 Service 코드용)
     */
//    public Questionnaire(Long userId, String questionnaireVersion, String form) {
//        this.userId = userId;
//        this.questionnaireVersion = questionnaireVersion;
//        this.form = form;
//        this.userOralStatusList = new ArrayList<>();
//    }
}
