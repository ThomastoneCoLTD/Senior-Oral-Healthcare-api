package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.dto.*;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PagingDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.userNumberFormat;
import static com.kaii.dentix.common.DocumentOptionalGenerator.yesNoFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.kaii.dentix.domain.admin.controller.AdminController.class)
@ExtendWith(RestDocumentationExtension.class)
public class AdminControllerTest {

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
    private com.kaii.dentix.domain.admin.application.AdminService adminService;

    // Helper: SignUpResponse 생성
    public AdminAuthDto.SignUpResponse adminSignUpResponse(){
        return AdminAuthDto.SignUpResponse.builder()
                .adminId(1L)
                .adminName("홍길동")
                .build();
    }

    // Helper: ModifyPasswordRequest 생성 (비밀번호 초기화 응답용)
    public AdminAuthDto.ModifyPasswordRequest modifyPasswordRequest(){
        AdminAuthDto.ModifyPasswordRequest request = new AdminAuthDto.ModifyPasswordRequest();
        request.setPassword("dentix2023!");
        return request;
    }

    // Helper: AutoLoginResponse 생성
    public AdminAuthDto.AutoLoginResponse adminAutoLoginResponse(){
        return AdminAuthDto.AutoLoginResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .adminId(1L)
                .adminName("김관리자")
                .adminIsSuper(YnType.N)
                .build();
    }

    /**
     * 관리자 등록
     */
    @Test
    public void adminSignUp() throws Exception {

        // given
        // 리턴 타입 변경: AdminSignUpDto -> AdminAuthDto.SignUpResponse
        given(adminService.adminSignUp(any(AdminAuthDto.SignUpRequest.class))).willReturn(adminSignUpResponse());

        // 요청 객체 변경: AdminSignUpRequest -> AdminAuthDto.SignUpRequest
        // 필드명 변경: adminName -> name, adminLoginIdentifier -> loginId 등
        AdminAuthDto.SignUpRequest request = AdminAuthDto.SignUpRequest.builder()
                .name("홍길동")
                .loginId("adminhong")
                .phoneNumber("01012345678")
                .password("password1234!")
                .findPwdQuestionId(1L)
                .findPwdAnswer("Answer")
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/admin/account")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "account.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/account",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                // 변경된 필드명 반영 (name, loginId, phoneNumber, password)
                                fieldWithPath("name").type(JsonFieldType.STRING).description("관리자 이름"),
                                fieldWithPath("loginId").type(JsonFieldType.STRING).description("관리자 아이디"),
                                fieldWithPath("phoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("관리자 연락처"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("관리자 비밀번호"),
                                fieldWithPath("findPwdQuestionId").type(JsonFieldType.NUMBER).description("비밀번호 찾기 질문 ID"),
                                fieldWithPath("findPwdAnswer").type(JsonFieldType.STRING).description("비밀번호 찾기 답변")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.adminName").type(JsonFieldType.STRING).description("관리자 이름")
                        )
                ));

        verify(adminService).adminSignUp(any(AdminAuthDto.SignUpRequest.class));
    }

    /**
     * 관리자 비밀번호 변경
     */
    @Test
    public void adminModifyPassword() throws Exception {

        // given
        String password = "qwer1234!";
        // 요청 객체 변경: AdminModifyPasswordRequest -> AdminAuthDto.ModifyPasswordRequest
        AdminAuthDto.ModifyPasswordRequest request = new AdminAuthDto.ModifyPasswordRequest();
        request.setPassword(password);

        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/admin/account/password")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "account-password-modify.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/account/password-modify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                // 필드명 변경: adminPassword -> password
                                fieldWithPath("password").type(JsonFieldType.STRING).description("변경할 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(adminService).adminModifyPassword(any(HttpServletRequest.class), any(AdminAuthDto.ModifyPasswordRequest.class));
    }

    /**
     * 관리자 삭제
     */
    @Test
    public void deleteAdmin() throws Exception {

        // given
        doNothing().when(adminService).adminDelete(any(Long.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.delete("/admin/account?adminId={adminId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-delete.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/account/delete",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("adminId").description("관리자 고유 번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(adminService).adminDelete(any(Long.class));
    }

    /**
     * 관리자 비밀번호 초기화
     */
    @Test
    public void adminPasswordReset() throws Exception {

        // given
        // 리턴 타입 변경: AdminPasswordResetDto -> AdminAuthDto.ModifyPasswordRequest
        given(adminService.adminPasswordReset(any(Long.class))).willReturn(modifyPasswordRequest());

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/admin/account/reset-password?adminId={adminId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "reset-password.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/account/reset-password",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("adminId").description("관리자 고유 번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                // 필드명 변경: adminPassword -> password
                                fieldWithPath("response.password").type(JsonFieldType.NULL).optional().description("초기화된 비밀번호")
                        )
                ));

        verify(adminService).adminPasswordReset(any(Long.class));
    }

    /**
     * 관리자 목록 조회
     * (AdminListDto는 통합되지 않았으므로 기존 유지)
     */
    @Test
    public void adminList() throws Exception {

        // given
        // 1. 결과 데이터 생성 (AdminDto.Summary 사용)
        AdminDto.Summary admin1 = AdminDto.Summary.builder()
                .adminId(1L)
                .loginId("adminHong")     // adminLoginIdentifier -> loginId
                .name("홍길동")            // adminName -> name
                .phoneNumber("01012345678") // adminPhoneNumber -> phoneNumber
                .createdDate("2023.01.02") // created -> createdDate
                .isSuper(YnType.N)
                .build();

        AdminDto.Summary admin2 = AdminDto.Summary.builder()
                .adminId(2L)
                .loginId("adminKim")
                .name("김길동")
                .phoneNumber("01022222222")
                .createdDate("2023.01.02")
                .isSuper(YnType.N)
                .build();

        // 2. 응답 DTO 생성 (AdminDto.ListResponse 사용)
        AdminDto.ListResponse response = AdminDto.ListResponse.builder()
                .paging(new PagingDTO(1, 1, 5))
                .adminList(List.of(admin1, admin2))
                .build();

        // 3. Service Mocking (파라미터 타입 변경: AdminDto.SearchRequest)
        given(adminService.adminList(any(AdminDto.SearchRequest.class))).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/admin/account/list?page={page}&size={size}", 1, 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "account-list.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/account/list",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("page").description("요청 페이지"),
                                parameterWithName("size").description("한 페이지에 가져올 목록 개수")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.paging").type(JsonFieldType.OBJECT).description("페이징 정보"),
                                fieldWithPath("response.paging.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("response.paging.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 개수"),
                                fieldWithPath("response.paging.totalElements").type(JsonFieldType.NUMBER).description("총 데이터 개수"),

                                //필드명 변경 반영
                                fieldWithPath("response.adminList[]").type(JsonFieldType.ARRAY).optional().description("관리자 계정 목록"),
                                fieldWithPath("response.adminList[].adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.adminList[].loginId").type(JsonFieldType.STRING).description("관리자 아이디"), // 변경됨
                                fieldWithPath("response.adminList[].name").type(JsonFieldType.STRING).description("관리자 이름"),   // 변경됨
                                fieldWithPath("response.adminList[].phoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("관리자 연락처"), // 변경됨
                                fieldWithPath("response.adminList[].createdDate").type(JsonFieldType.STRING).description("관리자 가입일"), // 변경됨
                                fieldWithPath("response.adminList[].isSuper").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("슈퍼 관리자 여부")
                        )
                ));

        verify(adminService).adminList(any(AdminDto.SearchRequest.class));
    }

    /**
     * 관리자 자동 로그인
     */
    @Test
    public void adminAutoLogin() throws Exception {

        // given
        // 리턴 타입 변경: AdminAutoLoginDto -> AdminAuthDto.AutoLoginResponse
        given(adminService.adminAutoLogin(any(HttpServletRequest.class))).willReturn(adminAutoLoginResponse());

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/admin/account/auto-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-auto-login.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/account/auto-login",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("response.adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.adminName").type(JsonFieldType.STRING).description("관리자 이름"),
                                fieldWithPath("response.adminIsSuper").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("관리자 슈퍼계정 여부")
                        )
                ));

        verify(adminService).adminAutoLogin(any(HttpServletRequest.class));
    }
}
