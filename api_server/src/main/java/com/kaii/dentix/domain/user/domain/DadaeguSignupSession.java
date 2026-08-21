package com.kaii.dentix.domain.user.domain;

import com.kaii.dentix.domain.type.GenderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "dadaegu_signup_session")
public class DadaeguSignupSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dadaeguSignupSessionId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 255)
    private String externalDid;

    @Column(nullable = false, length = 100)
    private String userName;

    @Column(nullable = false, length = 45)
    private String userPhoneNumber;

    @Column(nullable = false, length = 10)
    private String userBirthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GenderType userGender;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void consume(LocalDateTime now) {
        usedAt = now;
    }
}
