package com.kaii.dentix.domain.passwordReset;

import com.kaii.dentix.domain.passwordReset.application.PasswordResetService;
import com.kaii.dentix.domain.passwordReset.dao.PasswordResetTokenRepository;
import com.kaii.dentix.domain.passwordReset.domain.PasswordResetAccountType;
import com.kaii.dentix.domain.passwordReset.domain.PasswordResetToken;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordResetServiceTest {

    @Test
    void storesOnlyTokenHashWhenIssuingResetToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetService service = new PasswordResetService(repository, 600);

        var response = service.issue(PasswordResetAccountType.USER, 7L, "dentix123");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(response.getResetToken()).isNotBlank();
        assertThat(response.getExpiresInSeconds()).isEqualTo(600);
        assertThat(captor.getValue().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(response.getResetToken());
    }

    @Test
    void consumesValidTokenOnlyOnce() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetService service = new PasswordResetService(repository, 600);
        PasswordResetToken token = PasswordResetToken.builder()
                .accountType(PasswordResetAccountType.USER)
                .accountId(11L)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();
        given(repository.findByTokenHashAndAccountType(any(), eq(PasswordResetAccountType.USER)))
                .willReturn(Optional.of(token));

        assertThat(service.consume(PasswordResetAccountType.USER, "reset-token")).isEqualTo(11L);
        assertThatThrownBy(() -> service.consume(PasswordResetAccountType.USER, "reset-token"))
                .isInstanceOf(BadRequestApiException.class);
    }

    @Test
    void rejectsExpiredToken() {
        PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
        PasswordResetService service = new PasswordResetService(repository, 600);
        PasswordResetToken token = PasswordResetToken.builder()
                .accountType(PasswordResetAccountType.USER)
                .accountId(11L)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
        given(repository.findByTokenHashAndAccountType(any(), eq(PasswordResetAccountType.USER)))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume(PasswordResetAccountType.USER, "expired-token"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("만료");
    }
}
