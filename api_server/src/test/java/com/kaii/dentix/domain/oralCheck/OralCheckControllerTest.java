package com.kaii.dentix.domain.oralCheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.common.ControllerTest;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.controller.OralCheckController;
import com.kaii.dentix.domain.oralCheck.dto.OralCheckDto;
import com.kaii.dentix.domain.oralStatus.dto.OralStatusDto;
import com.kaii.dentix.domain.type.OralDateStatusType;
import com.kaii.dentix.domain.type.OralSectionType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.global.common.response.DataResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OralCheckController.class)
public class OralCheckControllerTest extends ControllerTest {

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
    private OralCheckService oralCheckService;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("구강 검진 사진 촬영/업로드")
    public void oralCheckPhoto() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes(StandardCharsets.UTF_8));

        // Mock Response (OralCheckDto.PhotoResponse)
        OralCheckDto.PhotoResponse mockResponse = OralCheckDto.PhotoResponse.builder()
                .organizationId(100L)
                .oralCheckId(1L)
                .success(true)
                .remainingResponses(5)
                .build();

        given(oralCheckService.oralCheckPhoto(any(HttpServletRequest.class), any(MultipartFile.class), anyString()))
                .willReturn(new DataResponse<>(mockResponse));

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.multipart("/oralCheck/photo")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer AccessToken")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("oralCheck/photo",
                        getDocumentResponse(),
                        requestParts(
                                partWithName("file").description("구강 촬영 이미지 파일")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.organizationId").type(JsonFieldType.NUMBER).description("기관 ID"),
                                fieldWithPath("response.oralCheckId").type(JsonFieldType.NUMBER).description("생성된 구강 검진 ID"),
                                fieldWithPath("response.success").type(JsonFieldType.BOOLEAN).description("분석 성공 여부"),
                                fieldWithPath("response.remainingResponses").type(JsonFieldType.NUMBER).description("남은 검진 횟수")
                        )
                ));
    }

    @Test
    @DisplayName("구강 검진 결과 상세 조회")
    public void oralCheckResult() throws Exception {
        // given
        OralCheckDto.ResultResponse mockResponse = OralCheckDto.ResultResponse.builder()
                .userId(1L)
                .organizationId(100L)
                .success(true)
                .oralCheckResultTotalType(OralCheckResultType.HEALTHY)
                .created(new Date())
                .oralCheckTotalRange(55.0f)
                .oralCheckUpRightRange(73.0f)
                .oralCheckUpRightScoreType(OralCheckResultType.GOOD)
                .oralCheckUpLeftRange(70.0f)
                .oralCheckUpLeftScoreType(OralCheckResultType.DANGER)
                .oralCheckDownLeftRange(16.0f)
                .oralCheckDownLeftScoreType(OralCheckResultType.ATTENTION)
                .oralCheckDownRightRange(20.0f)
                .oralCheckDownRightScoreType(OralCheckResultType.ATTENTION)
                .oralCheckCommentList(Collections.singletonList("우측 상악 관리가 필요합니다."))
                .remainingResponses(4)
                .build();
        given(oralCheckService.oralCheckResult(any(HttpServletRequest.class), anyLong()))
                .willReturn(mockResponse);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/oralCheck/result")
                        .param("oralCheckId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer AccessToken")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andDo(document("oralCheck/result",
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("oralCheckId").description("구강 검진 ID")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                fieldWithPath("response.organizationId").type(JsonFieldType.NUMBER).description("기관 ID"),
                                fieldWithPath("response.success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("response.oralCheckResultTotalType").type(JsonFieldType.STRING).description("종합 결과 타입"),
                                fieldWithPath("response.created").type(JsonFieldType.STRING).attributes(dateFormat()).description("검진 일시"),
                                fieldWithPath("response.oralCheckTotalRange").type(JsonFieldType.NUMBER).description("전체 플라그 비율"),
                                // (나머지 세부 필드들은 필요 시 추가 작성)
                                fieldWithPath("response.oralCheckUpRightRange").type(JsonFieldType.NUMBER).optional().description("우상 비율"),
                                fieldWithPath("response.oralCheckUpRightScoreType").type(JsonFieldType.STRING).optional().description("우상 점수 타입"),
                                fieldWithPath("response.oralCheckUpLeftRange").type(JsonFieldType.NUMBER).optional().description("좌상 비율"),
                                fieldWithPath("response.oralCheckUpLeftScoreType").type(JsonFieldType.STRING).optional().description("좌상 점수 타입"),
                                fieldWithPath("response.oralCheckDownLeftRange").type(JsonFieldType.NUMBER).optional().description("좌하 비율"),
                                fieldWithPath("response.oralCheckDownLeftScoreType").type(JsonFieldType.STRING).optional().description("좌하 점수 타입"),
                                fieldWithPath("response.oralCheckDownRightRange").type(JsonFieldType.NUMBER).optional().description("우하 비율"),
                                fieldWithPath("response.oralCheckDownRightScoreType").type(JsonFieldType.STRING).optional().description("우하 점수 타입"),
                                fieldWithPath("response.oralCheckCommentList").type(JsonFieldType.ARRAY).description("코멘트 목록"),
                                fieldWithPath("response.remainingResponses").type(JsonFieldType.NUMBER).optional().description("남은 횟수")
                        )
                ));
    }

    @Test
    @DisplayName("구강 상태 타임라인 조회")
    public void oralCheckTimeline() throws Exception {
        // given
        OralCheckDto.TimelineResponse mockResponse = OralCheckDto.TimelineResponse.builder()
                .sectionList(List.of(
                        OralCheckDto.Section.builder()
                                .sectionType(OralSectionType.ORAL_CHECK)
                                .date(new Date())
                                .timeInterval(3600L)
                                .build()
                ))
                .dailyList(List.of(
                        OralCheckDto.Daily.builder()
                                .date(new Date())
                                .status(OralDateStatusType.GOOD)
                                .questionnaire(true)
                                .detailList(List.of())
                                .build()
                ))
                .build();

        given(oralCheckService.oralCheck(any(HttpServletRequest.class)))
                .willReturn(mockResponse);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/oralCheck")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer AccessToken")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andDo(document("oralCheck/timeline", // 문서명
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.sectionList").type(JsonFieldType.ARRAY).description("상단 섹션 목록"),
                                fieldWithPath("response.sectionList[].sectionType").type(JsonFieldType.STRING).description("섹션 타입"),
                                fieldWithPath("response.sectionList[].date").type(JsonFieldType.STRING).optional().description("최근 날짜"),
                                fieldWithPath("response.sectionList[].timeInterval").type(JsonFieldType.NUMBER).optional().description("경과 시간(초)"),
                                fieldWithPath("response.sectionList[].sort").type(JsonFieldType.NUMBER).description("정렬 순서"),
                                fieldWithPath("response.sectionList[].toothBrushingList").type(JsonFieldType.ARRAY).optional().description("양치 목록"),

                                fieldWithPath("response.dailyList").type(JsonFieldType.ARRAY).description("일별 상세 목록"),
                                fieldWithPath("response.dailyList[].date").type(JsonFieldType.STRING).description("날짜"),
                                fieldWithPath("response.dailyList[].status").type(JsonFieldType.STRING).optional().description("일별 상태"),
                                fieldWithPath("response.dailyList[].questionnaire").type(JsonFieldType.BOOLEAN).description("문진표 작성 여부"),
                                fieldWithPath("response.dailyList[].detailList").type(JsonFieldType.ARRAY).description("상세 활동 목록")
                        )
                ));
    }


    /**
     * 대시보드 조회
     */
    @Test
    @DisplayName("대시보드 조회")
    public void dashboard() throws Exception {
        // given
        OralCheckDto.DashboardResponse mockResponse = OralCheckDto.DashboardResponse.builder()
                .latestOralCheckId(1L)
                .oralCheckTimeInterval(120L)
                .oralCheckTotalCount(10)
                .oralStatus(new OralStatusDto.OralStatusType("A", "건강형"))
                .build();

        given(oralCheckService.dashboard(any(HttpServletRequest.class)))
                .willReturn(mockResponse);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/oralCheck/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer AccessToken")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andDo(document("oralCheck/dashboard",
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.latestOralCheckId").type(JsonFieldType.NUMBER).optional().description("최근 구강 검진 ID"),
                                fieldWithPath("response.oralCheckTimeInterval").type(JsonFieldType.NUMBER).optional().description("경과 시간"),
                                fieldWithPath("response.oralCheckTotalCount").type(JsonFieldType.NUMBER).description("총 검진 횟수"),
                                fieldWithPath("response.oralCheckHealthyCount").type(JsonFieldType.NUMBER).description("구강 촬영 건강 횟수"),
                                fieldWithPath("response.oralCheckGoodCount").type(JsonFieldType.NUMBER).description("구강 촬영 양호 횟수"),
                                fieldWithPath("response.oralCheckAttentionCount").type(JsonFieldType.NUMBER).description("구강 촬영 주의 횟수"),
                                fieldWithPath("response.oralCheckDangerCount").type(JsonFieldType.NUMBER).description("구강 촬영 위험 횟수"),
                                fieldWithPath("response.toothBrushingTotalCount").type(JsonFieldType.NUMBER).description("양치 횟수"),
                                fieldWithPath("response.toothBrushingAverage").type(JsonFieldType.NUMBER).description("양치 일 평균"),
                                fieldWithPath("response.oralStatus").type(JsonFieldType.OBJECT).optional().description("구강 상태"),
                                fieldWithPath("response.oralStatus.type").type(JsonFieldType.STRING).description("구강 상태 타입"),
                                fieldWithPath("response.oralStatus.title").type(JsonFieldType.STRING).description("구강 상태 제목"),
                                fieldWithPath("response.questionnaireCreated").type(JsonFieldType.STRING).optional().attributes(dateTimeFormat()).description("최근 문진표 검사일"),
                                fieldWithPath("response.oralCheckResultTotalType").type(JsonFieldType.STRING).optional().attributes(oralCheckResultTypeFormat()).description("최근 구강상태"),
                                fieldWithPath("response.oralCheckUpRightScoreType").type(JsonFieldType.STRING).optional().attributes(oralCheckResultTypeFormat()).description("상악우측 상태"),
                                fieldWithPath("response.oralCheckUpLeftScoreType").type(JsonFieldType.STRING).optional().attributes(oralCheckResultTypeFormat()).description("상악좌측 상태"),
                                fieldWithPath("response.oralCheckDownLeftScoreType").type(JsonFieldType.STRING).optional().attributes(oralCheckResultTypeFormat()).description("하악좌측 상태"),
                                fieldWithPath("response.oralCheckDownRightScoreType").type(JsonFieldType.STRING).optional().attributes(oralCheckResultTypeFormat()).description("하악우측 상태"),
                                fieldWithPath("response.oralCheckDailyList").type(JsonFieldType.ARRAY).description("구강 상태 변화 추이"),
                                fieldWithPath("response.oralCheckDailyList[].oralCheckNumber").type(JsonFieldType.NUMBER).description("회차"),
                                fieldWithPath("response.oralCheckDailyList[].oralCheckResultTotalType").attributes(oralCheckResultTypeFormat()).description("구강상태")
                        )
                ));
        verify(oralCheckService).dashboard(any(HttpServletRequest.class));
    }
}