package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.controller.AdminLoginController;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto;
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

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminLoginController.class)
@ExtendWith(RestDocumentationExtension.class)
public class AdminLoginControllerTest {

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
    private AdminLoginService adminLoginService;

    private AdminAuthDto.FindPasswordResponse adminFindPasswordResponse() {
        return AdminAuthDto.FindPasswordResponse.builder()
                .adminId(1L)
                .adminName("홍길동")
                .loginId("adminhong")
                .build();
    }

    @Test
    public void adminLogin() throws Exception {
        AdminAuthDto.FindPasswordRequest request = new AdminAuthDto.FindPasswordRequest();
        request.setLoginId("adminhong");
        request.setQuestionId(1L);
        request.setAnswer("초등학교");

        given(adminLoginService.adminFindPassword(any(AdminAuthDto.FindPasswordRequest.class)))
                .willReturn(adminFindPasswordResponse());

        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/admin/find-password")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("admin/find-password",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("loginId").type(JsonFieldType.STRING).description("관리자 아이디"),
                                fieldWithPath("questionId").type(JsonFieldType.NUMBER).description("비밀번호 찾기 질문 ID"),
                                fieldWithPath("answer").type(JsonFieldType.STRING).description("비밀번호 찾기 답변")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.adminName").type(JsonFieldType.STRING).description("관리자 이름"),
                                fieldWithPath("response.loginId").type(JsonFieldType.STRING).description("관리자 아이디")
                        )
                ));

        verify(adminLoginService).adminFindPassword(any(AdminAuthDto.FindPasswordRequest.class));
    }
}
