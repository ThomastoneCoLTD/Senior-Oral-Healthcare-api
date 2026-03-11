package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.application.AdminStatisticService;
import com.kaii.dentix.domain.admin.controller.AdminStatisticController;
import com.kaii.dentix.domain.admin.dto.AdminStatisticDto;
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

import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatisticController.class)
@ExtendWith(RestDocumentationExtension.class)
public class AdminStatisticControllerTest {

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

    @Test
    public void getOrgStatistics() throws Exception {
        AdminStatisticDto.OrgUserStatsResponse response = AdminStatisticDto.OrgUserStatsResponse.builder()
                .organizationName("테스트 치과")
                .totalUsers(100)
                .maleUsers(40)
                .femaleUsers(60)
                .newUsers(5)
                .oralCheckStats(List.of(
                        AdminStatisticDto.OralCheckStats.builder()
                                .countHealthy(10)
                                .countGood(30)
                                .countAttention(50)
                                .countDanger(10)
                                .build()
                ))
                .build();

        given(adminStatisticService.getOrganizationUserStatistics(any()))
                .willReturn(response);

        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/admin/statistic/org/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-statistic.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("admin/statistic",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.organizationName").type(JsonFieldType.STRING).description("기관명"),
                                fieldWithPath("response.totalUsers").type(JsonFieldType.NUMBER).description("전체 사용자 수"),
                                fieldWithPath("response.maleUsers").type(JsonFieldType.NUMBER).description("남성 사용자 수"),
                                fieldWithPath("response.femaleUsers").type(JsonFieldType.NUMBER).description("여성 사용자 수"),
                                fieldWithPath("response.newUsers").type(JsonFieldType.NUMBER).description("최근 가입 사용자 수"),
                                fieldWithPath("response.oralCheckStats").type(JsonFieldType.ARRAY).description("구강 검사 상태 통계"),
                                fieldWithPath("response.oralCheckStats[].countHealthy").type(JsonFieldType.NUMBER).description("'건강' 횟수"),
                                fieldWithPath("response.oralCheckStats[].countGood").type(JsonFieldType.NUMBER).description("'양호' 횟수"),
                                fieldWithPath("response.oralCheckStats[].countAttention").type(JsonFieldType.NUMBER).description("'주의' 횟수"),
                                fieldWithPath("response.oralCheckStats[].countDanger").type(JsonFieldType.NUMBER).description("'위험' 횟수"),
                                fieldWithPath("response.oralCheckStats[].total").type(JsonFieldType.NUMBER).description("총합")
                        )
                ));

        verify(adminStatisticService).getOrganizationUserStatistics(any());
    }
}
