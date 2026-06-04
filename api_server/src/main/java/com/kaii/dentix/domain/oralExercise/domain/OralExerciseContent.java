package com.kaii.dentix.domain.oralExercise.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oral_exercise_content")
public class OralExerciseContent extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oralExerciseContentId;

    @Column(nullable = false)
    private int contentSort;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 500)
    private String learningPoint;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 500)
    private String videoUrl;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false, length = 45)
    private String level;

    @Column(nullable = false)
    private boolean active;

}
