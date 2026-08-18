package com.kaii.dentix.global.common.util;

import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(
        value = AiModelService.class,
        properties = {
                "aiModel.apiUrl.oralCheck=https://oral-check-ai.test/analyze",
                "aiModel.apiUrl.gingivitis=https://gingivitis-ai.test/analyze",
                "aiModel.apiUrl.questionnaire=https://questionnaire-ai.test/analyze"
        }
)
class AiModelServiceTest {
    private static final String QUESTIONNAIRE_AI_URL = "https://questionnaire-ai.test/analyze";

    @Autowired
    private AiModelService aiModelService;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void sendsQuestionnaireSurveyAsUrlEncodedJson() {
        ReflectionTestUtils.setField(aiModelService, "questionnaireAiModelApiUrl", QUESTIONNAIRE_AI_URL);
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("q_1", 4);
        form.put("q_3", List.of(5, 7));
        Map<String, Map<String, Object>> survey = Map.of("form", form);

        server.expect(once(), requestTo(QUESTIONNAIRE_AI_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("survey=%7B")))
                .andRespond(withSuccess(
                        "{\"status_code\":200,\"status_msg\":\"OK\",\"contents_type\":[\"A\"]}",
                        MediaType.APPLICATION_JSON
                ));

        QuestionnaireAnalysisResponse response = aiModelService.getQuestionnaireAiModel(survey).join();

        assertThat(response.getContentsType()).containsExactly("A");
        server.verify();
    }
}
