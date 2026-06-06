package com.kaii.dentix.domain.oralExercise.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oral_exercise_interaction_log")
public class OralExerciseInteractionLog extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oralExerciseInteractionLogId;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oral_exercise_content_id", nullable = false)
    private OralExerciseContent content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OralExerciseInteractionEventType eventType;

    @Column(nullable = false)
    private int watchedSeconds;

    @Column(nullable = false)
    private int currentPositionSeconds;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private int completionRate;

    @Column(nullable = false)
    private boolean completed;

    @Column(length = 100)
    private String sessionId;
}
