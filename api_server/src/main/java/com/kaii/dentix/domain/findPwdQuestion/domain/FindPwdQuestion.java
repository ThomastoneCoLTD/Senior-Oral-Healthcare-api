package com.kaii.dentix.domain.findPwdQuestion.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter @Builder
@AllArgsConstructor @NoArgsConstructor
@Table(name = "find_pwd_question")
public class FindPwdQuestion extends TimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long findPwdQuestionId;

    @Column(nullable = false)
    private Long findPwdQuestionSort;

    @Column(nullable = false)
    private String findPwdQuestionTitle;

}
