package com.kaii.dentix.domain.questionnaire;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.contents.dao.ContentsCustomRepository;
import com.kaii.dentix.domain.oralStatus.jpa.OralStatusRepository;
import com.kaii.dentix.domain.oralStatusAssignment.dao.OralStatusAssignmentRepository;
import com.kaii.dentix.domain.questionnaire.application.QuestionnaireFallbackAnalyzer;
import com.kaii.dentix.domain.questionnaire.application.QuestionnaireService;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireRepository;
import com.kaii.dentix.global.common.util.AiModelService;
import com.kaii.dentix.domain.user.application.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class QuestionnaireServiceTest {

    @Test
    void templateIsAvailableWithoutOrganizationSubscription() throws Exception {
        UserService userService = mock(UserService.class);
        QuestionnaireService service = new QuestionnaireService(
                userService,
                new ObjectMapper(),
                mock(AiModelService.class),
                mock(OralStatusRepository.class),
                mock(OralStatusAssignmentRepository.class),
                mock(QuestionnaireRepository.class),
                mock(ContentsCustomRepository.class),
                mock(QuestionnaireFallbackAnalyzer.class)
        );

        var template = service.getQuestionnaireTemplate(mock(HttpServletRequest.class));

        assertThat(template.getVersion()).isNotBlank();
        assertThat(template.getTemplate()).isNotEmpty();
        verifyNoInteractions(userService);
    }
}
