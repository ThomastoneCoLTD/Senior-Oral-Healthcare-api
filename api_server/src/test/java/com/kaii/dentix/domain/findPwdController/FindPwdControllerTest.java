package com.kaii.dentix.domain.findPwdController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.findPwdQuestion.application.FindPwdService;
import com.kaii.dentix.domain.findPwdQuestion.controller.FindPwdController;
import com.kaii.dentix.domain.findPwdQuestion.dto.FindPwdQuestionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FindPwdController.class)
@ExtendWith(RestDocumentationExtension.class)
public class FindPwdControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FindPwdService findPwdService;

    @Test
    public void userFindPwdQuestions() throws Exception{

        // given
        List<FindPwdQuestionDto.Question> questionsList = List.of(
                FindPwdQuestionDto.Question.builder()
                        .id(1L)
                        .sort(1L)
                        .title("내가 가장 좋아하는 색은?")
                        .build(),
                FindPwdQuestionDto.Question.builder()
                        .id(2L)
                        .sort(2L)
                        .title("내가 졸업한 초등학교의 이름은?")
                        .build(),
                FindPwdQuestionDto.Question.builder()
                        .id(3L)
                        .sort(3L)
                        .title("내가 가장 소중하게 생각하는 것은?")
                        .build()
        );

        given(findPwdService.userFindPwdQuestions()).willReturn(new FindPwdQuestionDto.Response(questionsList));

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/password/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("password/questions",
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.questions").type(JsonFieldType.ARRAY).description("비밀번호 찾기 질문 목록"),
                                fieldWithPath("response.questions[].id").type(JsonFieldType.NUMBER).description("비밀번호 찾기 질문 고유 번호"),
                                fieldWithPath("response.questions[].sort").type(JsonFieldType.NUMBER).description("비밀번호 찾기 질문 정렬 순서"),
                                fieldWithPath("response.questions[].title").type(JsonFieldType.STRING).description("비밀번호 찾기 질문")
                        )
                ));

        verify(findPwdService).userFindPwdQuestions();

    }

}
