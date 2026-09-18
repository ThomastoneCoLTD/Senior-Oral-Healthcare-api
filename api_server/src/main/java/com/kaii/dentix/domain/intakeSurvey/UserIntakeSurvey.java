package com.kaii.dentix.domain.intakeSurvey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "user_intake_survey")
@Getter
@NoArgsConstructor
public class UserIntakeSurvey {
    @Id
    private Long userId;
    @Column(nullable = false, length = 40)
    private String templateVersion;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answersJson;
    @Column(columnDefinition = "TEXT")
    private String scoresJson;
    @Column(nullable = false)
    private int currentTab;
    @Column(nullable = false)
    private Instant updatedAt;
    private Instant completedAt;
    @Version
    private Long revision;

    public UserIntakeSurvey(Long userId) { this.userId = userId; }

    public void save(String version, String answers, String scores, int tab, boolean complete) {
        this.templateVersion = version;
        this.answersJson = answers;
        this.scoresJson = scores;
        this.currentTab = tab;
        this.updatedAt = Instant.now();
        if (complete && this.completedAt == null) this.completedAt = this.updatedAt;
    }
}
