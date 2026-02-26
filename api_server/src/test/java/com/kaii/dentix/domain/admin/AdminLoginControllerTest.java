package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.admin.controller.AdminLoginController;
import com.kaii.dentix.domain.admin.dto.AdminAuthDto; //통합 DTO Import
import com.kaii.dentix.domain.type.YnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.yesNoFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminLoginController.class)
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
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AdminLoginService adminLoginService;

    //DTO 생성 헬퍼 메서드 수정
    public AdminAuthDto.LoginResponse adminLoginResponse() {
        return AdminAuthDto.LoginResponse.builder()
                .adminId(1L)
                .isFirstLogin(YnType.Y)
                .adminName("홍길동") // adminName -> name
                .accessToken("AccessToken")
                .refreshToken("RefreshToken")
                .adminIsSuper(YnType.N) // adminIsSuper -> isSuper
                .organizationId(10L)
                .organizationName("테스트 치과")
                .organizationSubscription(null) // 테스트용 null 처리
                .build();
    }

    /**
     * 관리자 로그인
     */
    @Test
    public void adminLogin() throws Exception {

        // given
        //파라미터 타입 및 리턴 타입 변경
        given(adminLoginService.login(any(AdminAuthDto.LoginRequest.class))).willReturn(adminLoginResponse());

        String password = "dentix2023!";

        //Request DTO 변경 (빌더 필드명 변경 확인)
        AdminAuthDto.LoginRequest request = AdminAuthDto.LoginRequest.builder()
                .loginId("adminhong") // adminLoginIdentifier -> loginId
                .password(password)   // adminPassword -> password
                .build();

        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/admin/login")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("admin/login",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                //요청 필드명 변경 반영
                                fieldWithPath("loginId").type(JsonFieldType.STRING).description("관리자 아이디"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("관리자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),

                                //응답 필드명 변경 반영
                                fieldWithPath("response.adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.name").type(JsonFieldType.STRING).description("관리자 이름"), // adminName -> name
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("response.isFirstLogin").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("최초 로그인 여부"),
                                fieldWithPath("response.isSuper").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("관리자 슈퍼계정 여부"), // adminIsSuper -> isSuper

                                //추가된 필드 (DTO 정의에 따라 optional 처리)
                                fieldWithPath("response.organizationId").type(JsonFieldType.NUMBER).optional().description("기관 ID"),
                                fieldWithPath("response.organizationName").type(JsonFieldType.STRING).optional().description("기관명"),
                                fieldWithPath("response.organizationSubscription").type(JsonFieldType.OBJECT).optional().description("기관 구독 정보")
                        )
                ));

        verify(adminLoginService).login(any(AdminAuthDto.LoginRequest.class));
    }
}