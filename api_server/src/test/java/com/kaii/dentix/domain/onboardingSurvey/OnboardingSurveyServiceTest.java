package com.kaii.dentix.domain.onboardingSurvey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.dao.UserRepository;
import com.kaii.dentix.domain.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.kaii.dentix.domain.onboardingSurvey.OnboardingSurveyDto.*;

class OnboardingSurveyServiceTest {
    private final UserService users = mock(UserService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OnboardingSurveyRepository repository = mock(OnboardingSurveyRepository.class);
    private final HttpServletRequest http = mock(HttpServletRequest.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private OnboardingSurveyTemplate template;
    private OnboardingSurveyService service;
    private User user;
    @BeforeEach void setup() throws Exception {
        template = new OnboardingSurveyTemplate(mapper);
        service = new OnboardingSurveyService(users, userRepository, repository, template, mapper);
        user = User.builder().userId(123L).onboardingSurveyRequired(true).build();
        when(users.getTokenUser(http)).thenReturn(user);
        when(userRepository.findByIdForUpdate(123L)).thenReturn(Optional.of(user));
    }
    @Test void existingMembersAreExemptAndNewMembersRemainRequiredUntilSaved() {
        user.setOnboardingSurveyRequired(null); assertThat(service.status(http).required()).isFalse();
        user.setOnboardingSurveyRequired(false); assertThat(service.status(http).required()).isFalse();
        user.setOnboardingSurveyRequired(true); assertThat(service.status(http).required()).isTrue();
        when(repository.existsById(123L)).thenReturn(true);
        assertThat(service.status(http)).isEqualTo(new Status(false, true));
    }
    @Test void incompleteSubmissionNeverWritesOrCompletes() {
        assertThatThrownBy(() -> service.submit(http, new SubmitRequest(template.get().version(), Map.of())))
                .isInstanceOf(SurveyValidationException.class);
        verify(repository, never()).saveAndFlush(any());
        assertThat(service.status(http).required()).isTrue();
    }
    @Test void existingMembersCannotSubmit() {
        user.setOnboardingSurveyRequired(null);
        assertThatThrownBy(() -> service.submit(http, null)).isInstanceOf(SurveyValidationException.class);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void retriedSubmissionDoesNotOverwriteExistingAnswers() throws Exception {
        when(repository.existsById(123L)).thenReturn(true);
        assertThat(service.submit(http, null).completed()).isTrue();
        verify(userRepository).findByIdForUpdate(123L);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void savesAuthenticatedMemberAnswersScoresVersionAndTimestamp() throws Exception {
        Map<String, List<String>> answers = new LinkedHashMap<>();
        template.get().sections().forEach(s -> s.questions().forEach(q -> answers.put(q.key(),
                List.of(q.type().equals("month") ? "unknown" : q.options().get(0).value()))));
        assertThat(service.submit(http, new SubmitRequest(template.get().version(), answers)).completed()).isTrue();
        var captor = ArgumentCaptor.forClass(OnboardingSurvey.class);
        verify(repository).saveAndFlush(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(123L);
        assertThat(saved.getVersion()).isEqualTo(template.get().version());
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(mapper.readTree(saved.getAnswers()).size()).isEqualTo(46);
        assertThat(mapper.readTree(saved.getScores()).get("eat10").asInt()).isZero();
    }
}
