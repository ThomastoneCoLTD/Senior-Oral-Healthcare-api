package com.kaii.dentix.domain.onboardingSurvey;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "onboarding_survey")
public class OnboardingSurvey {
    // One immutable submission per member, including concurrent/retried requests.
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false, length = 40)
    private String version;
    @Column(nullable = false, columnDefinition = "json")
    private String answers;
    @Column(nullable = false, columnDefinition = "json")
    private String scores;
    @Column(nullable = false)
    private Instant submittedAt;

    public OnboardingSurvey(Long userId, String version, String answers, String scores) {
        this.userId = userId;
        this.version = version;
        this.answers = answers;
        this.scores = scores;
        this.submittedAt = Instant.now();
    }
}
