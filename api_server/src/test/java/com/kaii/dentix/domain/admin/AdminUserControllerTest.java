package com.kaii.dentix.domain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.application.AdminUserService;
import com.kaii.dentix.domain.admin.controller.AdminUserController;
import com.kaii.dentix.domain.admin.dto.AdminUserDto; //통합 DTO Import
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;
import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.*;
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

@WebMvcTest(AdminUserController.class)
@ExtendWith(RestDocumentationExtension.class)
public class AdminUserControllerTest {

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
    private com.kaii.dentix.domain.admin.application.AdminService adminService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private com.kaii.dentix.domain.jwt.JwtTokenUtil jwtTokenUtil;

    /**
     * 사용자 인증
     */
    @Test
    public void userVerify() throws Exception {
        // given
        doNothing().when(adminUserService).userVerify(any(Long.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/admin/user/verify?userId={userId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-verify.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/user/verify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("userId").description("사용자 고유 번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(adminUserService).userVerify(any(Long.class));
    }

    /**
     * 사용자 정보 조회 (수정용 상세 정보)
     */
    @Test
    public void userInfo() throws Exception {

        // given
        //DTO 변경: AdminUserModifyInfoDto -> AdminUserDto.DetailResponse
        AdminUserDto.DetailResponse userInfo = AdminUserDto.DetailResponse.builder()
                .loginId("dentix123")
                .name("김덴티")
                .gender(GenderType.M)
                .build();

        given(adminUserService.userInfo(any(Long.class))).willReturn(userInfo);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/admin/user/info?userId={userId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-user-info.이호준.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/user/info",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("userId").description("사용자 고유번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                //필드명 변경 반영 (loginId, name, gender)
                                fieldWithPath("response.loginId").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("response.name").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("response.gender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")
                        )
                ));

        verify(adminUserService).userInfo(any(Long.class));
    }

    /**
     * 사용자 정보 수정
     */
    @Test
    public void userModify() throws Exception {

        // given
        //DTO 변경: AdminUserModifyRequest -> AdminUserDto.ModifyRequest
        AdminUserDto.ModifyRequest request = AdminUserDto.ModifyRequest.builder()
                .userId(1L)
                .loginId("dentix123")
                .name("김덴티")
                .gender(GenderType.M)
                .build();

        doNothing().when(adminUserService).userModify(any(AdminUserDto.ModifyRequest.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/admin/user")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-user-modify.이호준.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/user/modify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                //필드명 변경 반영
                                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("사용자 고유번호"),
                                fieldWithPath("loginId").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("gender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(adminUserService).userModify(any(AdminUserDto.ModifyRequest.class));
    }

    /**
     * 사용자 삭제
     */
    @Test
    public void userDelete() throws Exception {

        // given
        doNothing().when(adminUserService).userDelete(any(Long.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.delete("/admin/user?userId={userId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-delete.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andDo(document("admin/user/delete",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("userId").description("사용자 고유 번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(adminUserService).userDelete(any(Long.class));
    }

    /**
     * 사용자 목록 조회
     */
    @Test
    public void userList() throws Exception {

        Date date = new Date();

        // given
        // 1. Info 객체 생성 (Setter 사용 또는 생성자)
        AdminUserDto.Info user1 = new AdminUserDto.Info();
        user1.setUserId(1L);
        user1.setLoginId("dentix123");
        user1.setName("김덴티");
        user1.setGender(GenderType.M);
        user1.setOralCheckResultTotalType(OralCheckResultType.HEALTHY);
        user1.setOralCheckDate(date);
        user1.setIsVerify(YnType.Y);
        user1.setServiceNames(List.of("치은염 검출"));

        AdminUserDto.Info user2 = new AdminUserDto.Info();
        user2.setUserId(2L);
        user2.setLoginId("test1234");
        user2.setName("홍길동");
        user2.setGender(GenderType.W);
        user2.setOralCheckResultTotalType(OralCheckResultType.HEALTHY);
        user2.setOralCheckDate(date);
        user2.setIsVerify(YnType.Y);
        user2.setServiceNames(List.of());

        // 2. Response DTO 생성
        AdminUserDto.ListResponse response = AdminUserDto.ListResponse.builder()
                .paging(new PagingDTO(1, 2, 15))
                .userList(List.of(user1, user2))
                .build();

        given(adminUserService.userList(
                any(AdminUserDto.SearchRequest.class), //Request DTO 변경
                any(HttpServletRequest.class)
        )).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/admin/user?page={page}&size={size}", 1, 10)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "admin-user-list.고유경.AccessToken")
                        .with(user("user").roles("ADMIN"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("admin/user/list",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("page").description("요청 페이지"),
                                parameterWithName("size").description("한 페이지에 가져올 목록 개수"),
                                //파라미터명 변경 반영 (keyword, gender, datePeriodType 등)
                                parameterWithName("keyword").optional().description("아이디 혹은 이름"),
                                parameterWithName("oralCheckResultTotalType").optional().attributes(oralCheckResultTypeFormat()).description("구강 상태"),
                                parameterWithName("oralStatus").optional().description("문진표 유형"),
                                parameterWithName("gender").optional().attributes(genderFormat()).description("사용자 성별"),
                                parameterWithName("isVerify").optional().attributes(yesNoFormat()).description("사용자 인증 여부"),
                                parameterWithName("datePeriodType").optional().attributes(datePeriodTypeFormat()).description("기간 설정 타입 (구강 촬영일 or 문진표 검사일)"),
                                parameterWithName("startDate").optional().description("기간 설정 시작일"),
                                parameterWithName("endDate").optional().description("기간 설정 종료일"),
                                // organizationId는 @ModelAttribute에서 바인딩되므로 쿼리파라미터로 안 넘어오면 생략 가능 (테스트에선 주로 생략)
                                parameterWithName("organizationId").optional().description("기관 ID (일반관리자는 자동설정)")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.paging").type(JsonFieldType.OBJECT).description("페이징 정보"),
                                fieldWithPath("response.paging.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
                                fieldWithPath("response.paging.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 개수"),
                                fieldWithPath("response.paging.totalElements").type(JsonFieldType.NUMBER).description("총 데이터 개수"),

                                //응답 필드명 변경 반영
                                fieldWithPath("response.userList[]").type(JsonFieldType.ARRAY).description("사용자 계정 목록"),
                                fieldWithPath("response.userList[].userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("response.userList[].loginId").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("response.userList[].name").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("response.userList[].gender").optional().attributes(genderFormat()).description("사용자 성별"),
                                fieldWithPath("response.userList[].oralCheckResultTotalType").type(JsonFieldType.STRING).optional().attributes(oralCheckResultTypeFormat()).description("구강검진 결과"),
                                fieldWithPath("response.userList[].oralCheckDate").type(JsonFieldType.STRING).optional().attributes(dateFormat()).description("구강검진 검사일"),
                                fieldWithPath("response.userList[].isVerify").type(JsonFieldType.STRING).description("사용자 인증 여부"),
                                fieldWithPath("response.userList[].serviceNames").type(JsonFieldType.ARRAY).optional().description("사용중인 서비스 목록")
                        )
                ));

        verify(adminUserService).userList(any(AdminUserDto.SearchRequest.class), any(HttpServletRequest.class));
    }
}
