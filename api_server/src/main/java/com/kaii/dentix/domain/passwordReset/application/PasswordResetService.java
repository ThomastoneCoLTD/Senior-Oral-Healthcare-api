package com.kaii.dentix.domain.passwordReset.application;

import com.kaii.dentix.domain.passwordReset.dao.PasswordResetTokenRepository;
import com.kaii.dentix.domain.passwordReset.domain.PasswordResetAccountType;
import com.kaii.dentix.domain.passwordReset.domain.PasswordResetToken;
import com.kaii.dentix.domain.passwordReset.dto.PasswordResetDto;
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
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository repository;
    private final long ttlSeconds;

    public PasswordResetService(
            PasswordResetTokenRepository repository,
            @Value("${security.password-reset.ttl-seconds:600}") long ttlSeconds
    ) {
        this.repository = repository;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public PasswordResetDto.IssueResponse issue(
            PasswordResetAccountType accountType,
            Long accountId,
            String loginIdentifier
    ) {
        repository.deleteByAccount(accountType, accountId);

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        LocalDateTime now = LocalDateTime.now();

        repository.saveAndFlush(PasswordResetToken.builder()
                .accountType(accountType)
                .accountId(accountId)
                .tokenHash(hash(token))
                .expiresAt(now.plusSeconds(ttlSeconds))
                .createdAt(now)
                .build());

        return PasswordResetDto.IssueResponse.builder()
                .resetToken(token)
                .expiresInSeconds(ttlSeconds)
                .loginIdentifier(loginIdentifier)
                .build();
    }

    @Transactional
    public Long consume(PasswordResetAccountType accountType, String rawToken) {
        PasswordResetToken token = repository.findByTokenHashAndAccountType(hash(rawToken), accountType)
                .orElseThrow(() -> new BadRequestApiException("유효하지 않은 비밀번호 재설정 요청입니다."));
        LocalDateTime now = LocalDateTime.now();

        if (token.isUsed() || token.isExpired(now)) {
            throw new BadRequestApiException("비밀번호 재설정 요청이 만료되었거나 이미 사용되었습니다.");
        }

        token.consume(now);
        repository.saveAndFlush(token);
        return token.getAccountId();
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestApiException("비밀번호 재설정 토큰이 필요합니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Password reset hashing is unavailable.", exception);
        }
    }
}
