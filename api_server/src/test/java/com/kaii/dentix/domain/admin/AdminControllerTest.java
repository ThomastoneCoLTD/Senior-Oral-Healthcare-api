package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.admin.dto.*;
import com.kaii.dentix.common.ControllerTest;
import com.kaii.dentix.admin.application.AdminService;
import com.kaii.dentix.admin.controller.AdminController;
import com.kaii.dentix.admin.dto.request.AdminModifyPasswordRequest;
import com.kaii.dentix.admin.dto.request.AdminSignUpRequest;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.global.common.dto.PageAndSizeRequest;
import com.kaii.dentix.global.common.dto.PagingDTO;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;

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

@WebMvcTest(AdminController.class)
public class AdminControllerTest extends ControllerTest {

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
    private AdminService adminService;

    public AdminSignUpDto adminSignUpDto(){
        return AdminSignUpDto.builder()
                .adminId(1L)
                .adminPassword("dentix2023!")
                .build();
    }

    public AdminPasswordResetDto adminPasswordResetDto(){
        return AdminPasswordResetDto.builder()
                .adminPassword("dentix2023!")
                .build();
    }

    public AdminAutoLoginDto adminAutoLoginDto(){
        return AdminAutoLoginDto.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .adminId(1L)
                .adminName("김관리자")
                .adminIsSuper(YnType.N)
                .build();
    }

    /**
     *  관리자 등록
     */
    @Test
    public void adminSignUp() throws Exception{

        // given
        given(adminService.adminSignUp(any(AdminSignUpRequest.class))).willReturn(adminSignUpDto());

        AdminSignUpRequest adminSignUpRequest = AdminSignUpRequest.builder()
                .adminName("홍길동")
                .adminLoginIdentifier("adminhong")
                .adminPhoneNumber("01012345678")
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/admin/account")
                        .content(objectMapper.writeValueAsString(adminSignUpRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "account.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("admin/account",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("adminName").type(JsonFieldType.STRING).description("관리자 이름"),
                                fieldWithPath("adminLoginIdentifier").type(JsonFieldType.STRING).description("관리자 아이디"),
                                fieldWithPath("adminPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("관리자 연락처")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.adminPassword").type(JsonFieldType.STRING).description("관리자 초기 비밀번호")
                        )
                ));

        verify(adminService).adminSignUp(any(AdminSignUpRequest.class));

    }


    /**
     *  관리자 비밀번호 변경
     */
    @Test
    public void adminModifyPassword() throws Exception{

        // given
        String password = "qwer1234!";
        AdminModifyPasswordRequest request = new AdminModifyPasswordRequest(password);
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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("admin/account/password-modify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("adminPassword").type(JsonFieldType.STRING).description("관리자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(adminService).adminModifyPassword(any(HttpServletRequest.class), any(AdminModifyPasswordRequest.class));
    }

    /**
     *  관리자 삭제
     */
    @Test
    public void deleteAdmin() throws Exception{

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
     *  관리자 비밀번호 초기화
     */
    @Test
    public void adminPasswordReset() throws Exception{

        // given
        given(adminService.adminPasswordReset(any(Long.class))).willReturn(adminPasswordResetDto());

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                                fieldWithPath("response.adminPassword").type(JsonFieldType.STRING).description("관리자 비밀번호")
                        )
                ));

        verify(adminService).adminPasswordReset(any(Long.class));
    }

    /**
     *  관리자 목록 조회
     */
    @Test
    public void adminList() throws Exception{

        // given
        AdminListDto response = AdminListDto.builder()
                .paging(new PagingDTO(1, 1, 5))
                .adminList(new ArrayList<>(){{
                    add(new AdminAccountDto(1L, "adminHong", "홍길동", "01012345678", "2023.01.02"));
                    add(new AdminAccountDto(2L, "adminKim", "김길동", "01022222222", "2023.01.02"));
                }})
                .build();

        given(adminService.adminList(any(PageAndSizeRequest.class))).willReturn(response);

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
                                fieldWithPath("response.adminList[]").type(JsonFieldType.ARRAY).optional().description("관리자 계정 목록"),
                                fieldWithPath("response.adminList[].adminId").type(JsonFieldType.NUMBER).description("관리자 고유 번호"),
                                fieldWithPath("response.adminList[].adminLoginIdentifier").type(JsonFieldType.STRING).description("관리자 아이디"),
                                fieldWithPath("response.adminList[].adminName").type(JsonFieldType.STRING).description("관리자 이름"),
                                fieldWithPath("response.adminList[].adminPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("관리자 연락처"),
                                fieldWithPath("response.adminList[].created").type(JsonFieldType.STRING).description("관리자 가입일")
                        )
                ));

        verify(adminService).adminList(any(PageAndSizeRequest.class));

    }

    /**
     *  관리자 자동 로그인
     */
    @Test
    public void adminAutoLogin() throws Exception {

        // given
        given(adminService.adminAutoLogin(any(HttpServletRequest.class))).willReturn(adminAutoLoginDto());

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
