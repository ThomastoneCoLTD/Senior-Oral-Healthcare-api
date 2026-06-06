package com.kaii.dentix.domain.oralExercise.domain;

import com.kaii.dentix.global.common.entity.TimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_oral_exercise_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_oral_exercise_progress_user_content",
                        columnNames = {"user_id", "oral_exercise_content_id"}
                )
        }
)
public class UserOralExerciseProgress extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userOralExerciseProgressId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oral_exercise_content_id", nullable = false)
    private OralExerciseContent content;

    @Column(nullable = false)
    private int totalWatchedSeconds;

    @Column(nullable = false)
    private int maxWatchedSeconds;

    @Column(nullable = false)
    private int lastPositionSeconds;

    @Column(nullable = false)
    private int completionRate;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private int viewCount;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastViewedAt;

    public void updateProgress(
            int watchedSeconds,
            int currentPositionSeconds,
            int durationSeconds,
            int completionRate,
            boolean completed
    ) {
        this.totalWatchedSeconds += Math.max(watchedSeconds, 0);
        this.maxWatchedSeconds = Math.max(this.maxWatchedSeconds, Math.max(currentPositionSeconds, 0));
        this.lastPositionSeconds = Math.max(currentPositionSeconds, 0);
        this.completionRate = Math.max(this.completionRate, Math.max(completionRate, 0));
        this.completed = this.completed || completed || this.completionRate >= 95;
        this.viewCount += 1;
        this.lastViewedAt = new Date();

        if (durationSeconds > 0) {
            this.completionRate = Math.min(this.completionRate, 100);
        }
    }
}
