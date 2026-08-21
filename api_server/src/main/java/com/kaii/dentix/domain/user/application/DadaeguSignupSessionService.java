package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.dao.DadaeguSignupSessionRepository;
import com.kaii.dentix.domain.user.domain.DadaeguSignupSession;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class DadaeguSignupSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DadaeguSignupSessionRepository repository;
    private final long ttlSeconds;

    public DadaeguSignupSessionService(
            DadaeguSignupSessionRepository repository,
            @Value("${security.dadaegu-signup.ttl-seconds:600}") long ttlSeconds
    ) {
        this.repository = repository;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public IssueResult issue(
            String externalDid,
            String userName,
            String userPhoneNumber,
            String userBirthDate,
            GenderType userGender
    ) {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
        repository.deleteByExternalDid(externalDid);

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        LocalDateTime now = LocalDateTime.now();

        repository.saveAndFlush(DadaeguSignupSession.builder()
                .tokenHash(hash(token))
                .externalDid(externalDid)
                .userName(userName)
                .userPhoneNumber(userPhoneNumber)
                .userBirthDate(userBirthDate)
                .userGender(userGender)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .createdAt(now)
                .build());

        return new IssueResult(token, ttlSeconds);
    }

    @Transactional
    public DadaeguSignupSession consume(String rawToken) {
        DadaeguSignupSession session = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestApiException("유효하지 않은 다대구 가입 요청입니다."));
        LocalDateTime now = LocalDateTime.now();

        if (session.isUsed() || session.isExpired(now)) {
            throw new BadRequestApiException("다대구 가입 요청이 만료되었거나 이미 사용되었습니다.");
        }

        session.consume(now);
        repository.saveAndFlush(session);
        return session;
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestApiException("다대구 가입 진행 토큰이 필요합니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("DaDaegu signup token hashing is unavailable.", exception);
        }
    }

    public record IssueResult(String token, long expiresInSeconds) {
    }
}
