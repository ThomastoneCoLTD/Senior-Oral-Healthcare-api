package com.kaii.dentix.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.common.ControllerTest;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.controller.UserController;
import com.kaii.dentix.domain.user.dto.UserInfoDto;
import com.kaii.dentix.domain.user.dto.UserInfoModifyDto;
import com.kaii.dentix.domain.user.dto.UserInfoModifyQnADto;
import com.kaii.dentix.domain.user.dto.UserLoginDto;
import com.kaii.dentix.domain.user.dto.request.*;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserModifyServiceAgreeDto;
import com.kaii.dentix.domain.userServiceAgreement.dto.UserServiceAgreeList;
import com.kaii.dentix.domain.userServiceAgreement.dto.request.UserModifyServiceAgreeRequest;
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

import java.util.Arrays;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest extends ControllerTest {

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
    private UserService userService;

    private UserLoginDto userLoginDto(){
        return UserLoginDto.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .userId(1L)
                .userName("김덴티")
                .build();
    }

    private UserInfoModifyQnADto userInfoModifyQnADto(){
        return UserInfoModifyQnADto.builder()
                .findPwdQuestionId(2L)
                .findPwdAnswer("덴티엑스초등학교")
                .build();
    }

    private UserInfoModifyDto userInfoModifyDto(){
        return UserInfoModifyDto.builder()
                .userName("강덴티")
                .userGender(GenderType.W)
                .build();
    }

    private UserModifyServiceAgreeDto userModifyServiceAgreeDto(){
        Date date = new Date();
        return UserModifyServiceAgreeDto.builder()
                .serviceAgreeId(3L)
                .isUserServiceAgree(YnType.Y)
                .date(date)
                .build();
    }

    private UserInfoDto userInfoDto(){
        Date date = new Date();
        List<UserServiceAgreeList> userServiceAgreeLists = Arrays.asList(
                UserServiceAgreeList.builder()
                        .serviceAgreeId(3L)
                        .isUserServiceAgree(YnType.Y)
                        .date(date).build()
        );

        return UserInfoDto.builder()
                .userName("김덴티")
                .userLoginIdentifier("detix123")
                .patientPhoneNumber("01012345678")
                .userServiceAgreeLists(userServiceAgreeLists)
                .userGender(GenderType.W)
                .build();
    }

    /**
     *  사용자 자동 로그인
     */
    @Test
    public void userAutoLogin() throws Exception{

        // given
        given(userService.userAutoLogin(any(HttpServletRequest.class), any(UserAutoLoginRequest.class))).willReturn(userLoginDto());

        UserAutoLoginRequest userAutoLoginRequest = UserAutoLoginRequest.builder()
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user/auto-login")
                        .content(objectMapper.writeValueAsString(userAutoLoginRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/auto-login",
                        getDocumentRequest(),
                        getDocumentResponse(),

                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 닉네임")
                        )
                ));

        verify(userService).userAutoLogin(any(HttpServletRequest.class), any(UserAutoLoginRequest.class));

    }

    /**
     *  사용자 비밀번호 확인
     */
    @Test
    public void userPasswordVerify() throws Exception{

        // given
        doNothing().when(userService).userPasswordVerify(any(HttpServletRequest.class), any(UserPasswordVerifyRequest.class));

        String password = "password";
        UserPasswordVerifyRequest userPasswordVerifyRequest = UserPasswordVerifyRequest.builder()
                .userPassword(password)
                .build();
        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/user/password-verify")
                        .content(objectMapper.writeValueAsString(userPasswordVerifyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/password-verify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userPassword").type(JsonFieldType.STRING).description("사용자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userService).userPasswordVerify(any(HttpServletRequest.class), any(UserPasswordVerifyRequest.class));

    }

    /**
     *  사용자 보안정보수정 - 비밀번호 변경
     */
    @Test
    public void userModifyPassword() throws Exception{

        // given
        doNothing().when(userService).userModifyPassword(any(HttpServletRequest.class), any(UserInfoModifyPasswordRequest.class));

        String password = "password!";
        UserInfoModifyPasswordRequest userInfoModifyPasswordRequest = UserInfoModifyPasswordRequest.builder()
                .userPassword(password)
                .build();
        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user/password")
                        .content(objectMapper.writeValueAsString(userInfoModifyPasswordRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/password",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userPassword").type(JsonFieldType.STRING).description("사용자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userService).userModifyPassword(any(HttpServletRequest.class), any(UserInfoModifyPasswordRequest.class));

    }

    /**
     *  사용자 보안정보수정 - 질문과 답변 수정
     */
    @Test
    public void userModifyQnA() throws Exception {


        // given
        given(userService.userModifyQnA(any(HttpServletRequest.class), any(UserInfoModifyQnARequest.class))).willReturn(userInfoModifyQnADto());

        UserInfoModifyQnARequest userInfoModifyQnARequest = UserInfoModifyQnARequest.builder()
                .findPwdQuestionId(2L)
                .findPwdAnswer("덴티엑스초등학교")
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user/qna")
                        .content(objectMapper.writeValueAsString(userInfoModifyQnARequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/qna",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("findPwdQuestionId").type(JsonFieldType.NUMBER).description("사용자 비밀번호 찾기 질문"),
                                fieldWithPath("findPwdAnswer").type(JsonFieldType.STRING).description("사용자 비밀번호 찾기 답변")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.findPwdQuestionId").type(JsonFieldType.NUMBER).description("사용자 비밀번호 찾기 질문"),
                                fieldWithPath("response.findPwdAnswer").type(JsonFieldType.STRING).description("사용자 비밀번호 찾기 답변")
                        )
                ));

        verify(userService).userModifyQnA(any(HttpServletRequest.class), any(UserInfoModifyQnARequest.class));

    }

    /**
     *  사용자 회원정보 수정
     */
    @Test
    public void userModifyInfo() throws Exception{

        // given
        given(userService.userModifyInfo(any(HttpServletRequest.class), any(UserInfoModifyRequest.class))).willReturn(userInfoModifyDto());

        UserInfoModifyRequest userInfoModifyRequest = UserInfoModifyRequest.builder()
                .userName("강덴티")
                .userGender(GenderType.W)
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user")
                        .content(objectMapper.writeValueAsString(userInfoModifyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userName").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("response.userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")
                        )
                ));

        verify(userService).userModifyInfo(any(HttpServletRequest.class), any(UserInfoModifyRequest.class));

    }

    /**
     *  사용자 로그아웃
     */
    @Test
    public void userLogout() throws Exception{

        // given
        doNothing().when(userService).userLogout(any(HttpServletRequest.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/logout",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userService).userLogout(any(HttpServletRequest.class));

    }

    /**
     *  사용자 회원 탈퇴
     */
    @Test
    public void userRevoke() throws Exception{

        // given
        doNothing().when(userService).userRevoke(any(HttpServletRequest.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.delete("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/revoke",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userService).userRevoke(any(HttpServletRequest.class));

    }

    /**
     *  사용자 서비스 이용 동의 수정
     */
    @Test
    public void userModifyServiceAgree() throws Exception{

        // given
        given(userService.userModifyServiceAgree(any(HttpServletRequest.class), any(UserModifyServiceAgreeRequest.class))).willReturn(userModifyServiceAgreeDto());

        UserModifyServiceAgreeRequest userModifyServiceAgreeRequest = UserModifyServiceAgreeRequest.builder()
                .serviceAgreeId(3L)
                .isUserServiceAgree(YnType.Y)
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user/service-agreement")
                        .content(objectMapper.writeValueAsString(userModifyServiceAgreeRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/service-agreement",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("serviceAgreeId").type(JsonFieldType.NUMBER).description("서비스 이용 동의 고유 번호"),
                                fieldWithPath("isUserServiceAgree").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("사용자 서비스 이용 동의 여부")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.serviceAgreeId").type(JsonFieldType.NUMBER).description("서비스 이용 동의 고유 번호"),
                                fieldWithPath("response.isUserServiceAgree").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("사용자 서비스 이용 동의 여부"),
                                fieldWithPath("response.date").type(JsonFieldType.STRING).attributes(dateTimeFormat()).description("서비스 이용 동의 시각")
                        )
                ));

        verify(userService).userModifyServiceAgree(any(HttpServletRequest.class), any(UserModifyServiceAgreeRequest.class));

    }

    /**
     *  사용자 회원정보 조회
     */
    @Test
    public void userInfo() throws Exception{

        // given
        given(userService.userInfo(any(HttpServletRequest.class))).willReturn(userInfoDto());

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/info",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("response.userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("response.patientPhoneNumber").type(JsonFieldType.STRING).optional().attributes(userNumberFormat()).description("사용자(환자) 연락처"),
                                fieldWithPath("response.userServiceAgreeLists").type(JsonFieldType.ARRAY).description("사용자 서비스 이용동의 목록"),
                                fieldWithPath("response.userServiceAgreeLists[].serviceAgreeId").type(JsonFieldType.NUMBER).description("서비스 이용동의 고유 번호"),
                                fieldWithPath("response.userServiceAgreeLists[].isUserServiceAgree").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("사용자 서비스 이용동의 여부"),
                                fieldWithPath("response.userServiceAgreeLists[].date").type(JsonFieldType.STRING).attributes(dateTimeFormat()).optional().description("사용자 서비스 이용동의 수정 시각"),
                                fieldWithPath("response.userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")

                        )
                ));

        verify(userService).userInfo(any(HttpServletRequest.class));

    }

}
