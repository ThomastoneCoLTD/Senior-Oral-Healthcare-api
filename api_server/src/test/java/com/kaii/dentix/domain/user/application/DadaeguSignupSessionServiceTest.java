package com.kaii.dentix.domain.user.application;

import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.dao.DadaeguSignupSessionRepository;
import com.kaii.dentix.domain.user.domain.DadaeguSignupSession;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DadaeguSignupSessionServiceTest {

    private final DadaeguSignupSessionRepository repository = mock(DadaeguSignupSessionRepository.class);
    private final DadaeguSignupSessionService service = new DadaeguSignupSessionService(repository, 600);

    @Test
    void issueStoresOnlyTokenHashAndDeletesExpiredSessions() {
        DadaeguSignupSessionService.IssueResult result = service.issue(
                "did:daegu:test", "ci-hash", "홍길동", "01012345678", "1950-01-02", GenderType.M
        );

        ArgumentCaptor<DadaeguSignupSession> captor = ArgumentCaptor.forClass(DadaeguSignupSession.class);
        verify(repository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        verify(repository).deleteByExternalDid("did:daegu:test");
        verify(repository).saveAndFlush(captor.capture());
        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresInSeconds()).isEqualTo(600);
        assertThat(captor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(result.token());
        assertThat(captor.getValue().getCiHash()).isEqualTo("ci-hash");
    }

    @Test
    void consumeMarksValidSessionAsUsed() {
        DadaeguSignupSession session = DadaeguSignupSession.builder()
                .tokenHash("stored-hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(session));

        DadaeguSignupSession consumed = service.consume("raw-token");

        assertThat(consumed.getUsedAt()).isNotNull();
        verify(repository).saveAndFlush(session);
    }

    @Test
    void consumeRejectsExpiredSession() {
        DadaeguSignupSession session = DadaeguSignupSession.builder()
                .tokenHash("stored-hash")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.consume("raw-token"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("만료");
    }
}
