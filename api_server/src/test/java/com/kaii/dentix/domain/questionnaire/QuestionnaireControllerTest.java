package com.kaii.dentix.domain.questionnaire;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.contents.dto.ContentsDto;
import com.kaii.dentix.domain.oralStatus.dto.OralStatusDto;
import com.kaii.dentix.domain.questionnaire.application.QuestionnaireService;
import com.kaii.dentix.domain.questionnaire.controller.QuestionnaireController;
import com.kaii.dentix.domain.questionnaire.dto.QuestionnaireDto;
import com.kaii.dentix.domain.type.ContentsType;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.contentsTypeFormat;
import static com.kaii.dentix.common.DocumentOptionalGenerator.dateFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionnaireController.class)
@ExtendWith(RestDocumentationExtension.class)
public class QuestionnaireControllerTest {

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
    private QuestionnaireService questionnaireService;

    private QuestionnaireDto.TemplateJson questionnaireTemplateJsonDto() {
        List<QuestionnaireDto.Template> list = Arrays.asList(
                QuestionnaireDto.Template.builder()
                        .sort(1)
                        .key("q_1")
                        .number("01")
                        .title(Map.of("ko", "현재 구강 건강 상태는 어떻다고 생각하십니까?"))
                        .description(null)
                        .minimum(1)
                        .maximum(1)
                        .contents(Arrays.asList(
                                new QuestionnaireDto.TemplateContent(1, Map.of("ko", "매우 건강하다")),
                                new QuestionnaireDto.TemplateContent(2, Map.of("ko", "건강한 편이다")),
                                new QuestionnaireDto.TemplateContent(3, Map.of("ko", "보통이다")),
                                new QuestionnaireDto.TemplateContent(4, Map.of("ko", "건강하지 못한 편이다")),
                                new QuestionnaireDto.TemplateContent(5, Map.of("ko", "전혀 건강하지 않다"))
                        ))
                        .build(),
                QuestionnaireDto.Template.builder()
                        .sort(3)
                        .key("q_3")
                        .number("03")
                        .title(Map.of("ko", "지난 12개월 동안 구강관련 불편감이 있었습니까?"))
                        .description(Map.of("ko", "(중복 표시 가능)"))
                        .minimum(0)
                        .maximum(null)
                        .contents(Arrays.asList(
                                new QuestionnaireDto.TemplateContent(1, Map.of("ko", "아니요")),
                                new QuestionnaireDto.TemplateContent(2, Map.of("ko", "씹기 힘들다")),
                                new QuestionnaireDto.TemplateContent(3, Map.of("ko", "이가 아프다")),
                                new QuestionnaireDto.TemplateContent(4, Map.of("ko", "뜨겁고 찬 음식에 시리고 민감하다")),
                                new QuestionnaireDto.TemplateContent(5, Map.of("ko", "잇몸이 붓고 피가 난다")),
                                new QuestionnaireDto.TemplateContent(6, Map.of("ko", "입이 마른다")),
                                new QuestionnaireDto.TemplateContent(7, Map.of("ko", "입냄새가 난다"))
                        ))
                        .build()
        );

        return new QuestionnaireDto.TemplateJson("v1", list);
    }

    /**
     * 문진표 양식 조회
     */
    @Test
    public void questionnaireTemplate() throws Exception {
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/questionnaire/template")
                        .param("lang", "ko")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "questionnaire-template.이호준.AccessToken")
        );

        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("questionnaire/template",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("lang").optional().description("응답 언어 코드")
                        ),
                        responseFields(
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.version").type(JsonFieldType.STRING).description("템플릿 버전"),
                                fieldWithPath("response.template").type(JsonFieldType.ARRAY).description("문진표 양식"),
                                fieldWithPath("response.template[].sort").type(JsonFieldType.NUMBER).description("문항 정렬"),
                                fieldWithPath("response.template[].key").type(JsonFieldType.STRING).description("문항 고유번호 (제출 시 필요)"),
                                fieldWithPath("response.template[].number").type(JsonFieldType.STRING).description("문항 제목 번호"),
                                fieldWithPath("response.template[].title").type(JsonFieldType.STRING).description("문항 제목"),
                                fieldWithPath("response.template[].description").type(JsonFieldType.STRING).optional().description("문항 설명"),
                                fieldWithPath("response.template[].minimum").type(JsonFieldType.NUMBER).description("문항 최소 개수"),
                                fieldWithPath("response.template[].maximum").type(JsonFieldType.NUMBER).optional().description("문항 최대 개수 (null인 경우 무제한)"),
                                fieldWithPath("response.template[].contents").type(JsonFieldType.ARRAY).description("문항 선택지"),
                                fieldWithPath("response.template[].contents[].id").type(JsonFieldType.NUMBER).description("문항 선택지 고유번호 (제출 시 필요)"),
                                fieldWithPath("response.template[].contents[].text").type(JsonFieldType.STRING).description("문항 선택지 내용")
                        )
                ));
    }


    /**
     * 문진표 제출
     */
    @Test
    public void questionnaireSubmit() throws Exception {
        // 1. 데이터 생성 (QuestionnaireDto.Answer 사용)
        List<QuestionnaireDto.Answer> form = Arrays.asList(
                new QuestionnaireDto.Answer("q_1", new Integer[]{1}),
                new QuestionnaireDto.Answer("q_2", new Integer[]{2}),
                new QuestionnaireDto.Answer("q_3", new Integer[]{1, 2}),
                new QuestionnaireDto.Answer("q_4", new Integer[]{3}),
                new QuestionnaireDto.Answer("q_5", new Integer[]{3, 4}),
                new QuestionnaireDto.Answer("q_6", new Integer[]{5, 6}),
                new QuestionnaireDto.Answer("q_7", new Integer[]{0}),
                new QuestionnaireDto.Answer("q_8", new Integer[]{7, 8}),
                new QuestionnaireDto.Answer("q_9", new Integer[]{4}),
                new QuestionnaireDto.Answer("q_10", new Integer[]{1})
        );

        // Request 객체 생성 (QuestionnaireDto.SubmitRequest 사용)
        QuestionnaireDto.SubmitRequest request = new QuestionnaireDto.SubmitRequest(form);

        // given (Service Mocking 타입 변경)
        given(questionnaireService.questionnaireSubmit(any(HttpServletRequest.class), any(QuestionnaireDto.SubmitRequest.class)))
                .willReturn(new QuestionnaireDto.IdResponse(1L));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/questionnaire/submit")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "questionnaire-submit.이호준.AccessToken")
                //.with(user("user").roles("USER")) // 필요시 주석 해제
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("questionnaire/submit",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("form").type(JsonFieldType.ARRAY).description("문진표 항목"),
                                fieldWithPath("form[].key").type(JsonFieldType.STRING).description("문진표 key"),
                                fieldWithPath("form[].value").type(JsonFieldType.ARRAY).description("문진표 value")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.questionnaireId").type(JsonFieldType.NUMBER).description("문진표 고유번호")
                        )
                ));

        // verify 타입 변경
        verify(questionnaireService).questionnaireSubmit(any(HttpServletRequest.class), any(QuestionnaireDto.SubmitRequest.class));
    }
    /**
     * 문진표 결과 조회
     */
    @Test
    public void questionnaireResult() throws Exception {
        // 1. 구강 상태 정보 생성 (OralStatusDto.Info 사용)
        List<OralStatusDto.Info> oralStatusList = Arrays.asList(
                OralStatusDto.Info.builder()
                        .type("A")
                        .title("양치 관리형")
                        .description("양치 관리형은 현재 질환이 있거나 질환이 생길 수 있는 상태로...")
                        .subDescription("양치 관리형에 맞는 콘텐츠를 확인하신 후...")
                        .build(),
                OralStatusDto.Info.builder()
                        .type("B")
                        .title("충치 관리형")
                        .description("충치 관리형은 과거 충치 치료를 받았거나...")
                        .subDescription("충치 관리형에 맞는 콘텐츠를 확인하신 후...")
                        .build()
        );

        // 2. 콘텐츠 요약 정보 생성 (ContentsDto.Summary 사용)
        // * 주의: ResultResponse에서 categories 필드가 제거되었으므로 관련 데이터 생성을 생략합니다.
        List<ContentsDto.Summary> contents = Arrays.asList(
                ContentsDto.Summary.builder()
                        .id(1L)
                        .sort(1)
                        .title("백살도 거뜬한 건강한 치아관리 방법")
                        .type(ContentsType.CARD)
                        .typeColor("#FF9F06")
                        .thumbnail("https://dentix-api-dev.kai-i.com")
                        .videoURL(null)
                        .categoryIds(List.of(1, 2))
                        .build()
        );

        // given (반환 타입: QuestionnaireDto.ResultResponse)
        given(questionnaireService.questionnaireResult(any(HttpServletRequest.class), anyLong()))
                .willReturn(QuestionnaireDto.ResultResponse.builder()
                        .created(new Date())
                        .oralStatusList(oralStatusList)
                        .contents(contents)
                        .build());

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/questionnaire/result?questionnaireId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "questionnaire-result.이호준.AccessToken")
                //.with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("questionnaire/result",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("questionnaireId").description("문진표 고유번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.created").type(JsonFieldType.STRING).attributes(dateFormat()).description("문진표 제출일"),

                                // [수정] OralStatusDto.Info 필드 매핑
                                fieldWithPath("response.oralStatusList").type(JsonFieldType.ARRAY).description("구강 상태 목록"),
                                fieldWithPath("response.oralStatusList[].type").type(JsonFieldType.STRING).description("구강 상태 타입"),
                                fieldWithPath("response.oralStatusList[].title").type(JsonFieldType.STRING).description("구강 상태 제목"),
                                fieldWithPath("response.oralStatusList[].description").type(JsonFieldType.STRING).description("구강 상태 설명"),
                                fieldWithPath("response.oralStatusList[].subDescription").type(JsonFieldType.STRING).description("구강 상태 부연 설명"),

                                // [삭제] categories 관련 필드는 ResultResponse에서 제거되었으므로 문서화에서도 제외합니다.

                                // [수정] ContentsDto.Summary 필드 매핑
                                fieldWithPath("response.contents").type(JsonFieldType.ARRAY).description("콘텐츠 목록 (최대 2개)"),
                                fieldWithPath("response.contents[].id").type(JsonFieldType.NUMBER).description("콘텐츠 고유 번호"),
                                fieldWithPath("response.contents[].title").type(JsonFieldType.STRING).description("콘텐츠 제목"),
                                fieldWithPath("response.contents[].sort").type(JsonFieldType.NUMBER).description("콘텐츠 정렬 순서"),
                                fieldWithPath("response.contents[].type").type(JsonFieldType.STRING).attributes(contentsTypeFormat()).description("콘텐츠 타입"),
                                fieldWithPath("response.contents[].typeColor").type(JsonFieldType.STRING).description("콘텐츠 제목 색상"),
                                fieldWithPath("response.contents[].thumbnail").type(JsonFieldType.STRING).description("콘텐츠 썸네일"),
                                fieldWithPath("response.contents[].videoURL").type(JsonFieldType.NULL).optional().description("콘텐츠 동영상 경로"),
                                fieldWithPath("response.contents[].categoryIds").type(JsonFieldType.ARRAY).description("콘텐츠 카테고리 ID 목록")
                        )
                ));

        verify(questionnaireService).questionnaireResult(any(HttpServletRequest.class), anyLong());
    }
}
