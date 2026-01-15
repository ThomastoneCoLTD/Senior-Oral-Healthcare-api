package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.common.ControllerTest;
import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.controller.AdminStatisticController;
import com.kaii.dentix.domain.admin.dto.AdminStatisticDto; //통합 DTO Import
import com.kaii.dentix.domain.type.DatePeriodType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminStatisticController.class)
public class AdminStatisticControllerTest extends ControllerTest {

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
    private AdminStatisticService adminStatisticService;

    /**
     * 관리자 통계 조회 (기관용)
     */
    @Test
    public void getOrgStatistics() throws Exception {

        // given
        //통합 DTO(MainResponse)로 변경
        AdminStatisticDto.MainResponse response = AdminStatisticDto.MainResponse.builder()
                .userSignUpCount(new AdminStatisticDto.SignUpCount(30L, 20L, 10L)) // record 생성자 사용
                .averageState(OralCheckResultType.ATTENTION)
                .oralCheckCount(100)
                .oralCheckAverage(4)
                .oralCheckResultTypeCount(AdminStatisticDto.OralCheckStats.builder() // OralCheckStats 사용
                        .countHealthy(10)
                        .countGood(30)
                        .countAttention(50)
                        .countDanger(10)
                        .build())
                .questionnaireAllCount(50)
                .allQuestionnaireResultTypeCount(AdminStatisticDto.QuestionnaireStats.builder() // QuestionnaireStats 사용
                        .countA(10).countB(2).countC(3).countD(5).countE(10)
                        .countF(3).countG(2).countH(5).countI(5).countJ(3).countK(2)
                        .build())
                .build();

        //파라미터 타입 변경 (AdminStatisticDto.SearchRequest)
        given(adminStatisticService.getOrgStatistics(any(AdminStatisticDto.SearchRequest.class), any()))
                .willReturn(response);

        // when
        //파라미터명 변경 (allDatePeriod -> datePeriodType)
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/admin/statistic?datePeriodType={datePeriodType}&startDate={startDate}&endDate={endDate}",
                                DatePeriodType.MONTH1, "2023-01-01", "2023-09-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-statistic.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("admin/statistic",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                //파라미터명 변경 반영
                                parameterWithName("datePeriodType").optional().attributes(datePeriodTypeFormat()).description("기간 설정 타입 (구강 촬영일 or 문진표 검사일)"),
                                parameterWithName("startDate").optional().attributes(dateFormat()).description("기간 검색 시작일"),
                                parameterWithName("endDate").optional().attributes(dateFormat()).description("기간 검색 종료일")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userSignUpCount").type(JsonFieldType.OBJECT).description("전체 남녀 가입률"),
                                fieldWithPath("response.userSignUpCount.countAll").type(JsonFieldType.NUMBER).description("전체 가입자 수"),
                                fieldWithPath("response.userSignUpCount.countMan").type(JsonFieldType.NUMBER).description("남성 가입자 수"),
                                fieldWithPath("response.userSignUpCount.countWoman").type(JsonFieldType.NUMBER).description("여성 가입자 수"),
                                fieldWithPath("response.averageState").type(JsonFieldType.STRING).attributes(oralCheckResultTypeFormat()).description("평균 구강 상태"),
                                fieldWithPath("response.oralCheckCount").type(JsonFieldType.NUMBER).description("전체 구강검진 횟수"),
                                fieldWithPath("response.oralCheckAverage").type(JsonFieldType.NUMBER).description("사용자당 평균 구강검진 횟수"),
                                fieldWithPath("response.oralCheckResultTypeCount").type(JsonFieldType.OBJECT).description("구강검진 결과 타입별 횟수"),
                                fieldWithPath("response.oralCheckResultTypeCount.countHealthy").type(JsonFieldType.NUMBER).description("'건강' 횟수"),
                                fieldWithPath("response.oralCheckResultTypeCount.countGood").type(JsonFieldType.NUMBER).description("'양호' 횟수"),
                                fieldWithPath("response.oralCheckResultTypeCount.countAttention").type(JsonFieldType.NUMBER).description("'주의' 횟수"),
                                fieldWithPath("response.oralCheckResultTypeCount.countDanger").type(JsonFieldType.NUMBER).description("'위험' 횟수"),
                                fieldWithPath("response.questionnaireAllCount").type(JsonFieldType.NUMBER).description("전체 문진표 작성 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount").type(JsonFieldType.OBJECT).description("문진표 결과 타입별 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countA").type(JsonFieldType.NUMBER).description("'A' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countB").type(JsonFieldType.NUMBER).description("'B' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countC").type(JsonFieldType.NUMBER).description("'C' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countD").type(JsonFieldType.NUMBER).description("'D' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countE").type(JsonFieldType.NUMBER).description("'E' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countF").type(JsonFieldType.NUMBER).description("'F' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countG").type(JsonFieldType.NUMBER).description("'G' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countH").type(JsonFieldType.NUMBER).description("'H' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countI").type(JsonFieldType.NUMBER).description("'I' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countJ").type(JsonFieldType.NUMBER).description("'J' 횟수"),
                                fieldWithPath("response.allQuestionnaireResultTypeCount.countK").type(JsonFieldType.NUMBER).description("'K' 횟수")
                        )
                ));

        verify(adminStatisticService).getOrgStatistics(any(AdminStatisticDto.SearchRequest.class), any());
    }
}