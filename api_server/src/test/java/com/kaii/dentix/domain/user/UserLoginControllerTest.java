package com.kaii.dentix.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.common.ControllerTest;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.controller.UserLoginController;
import com.kaii.dentix.domain.user.dto.*;
import com.kaii.dentix.domain.user.dto.request.*;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Arrays;
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
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserLoginController.class)
public class UserLoginControllerTest extends ControllerTest{

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
    private UserLoginService userLoginService;

    private UserVerifyDto userVerifyDto(){
        return UserVerifyDto.builder()
                .userId(1L)
//                .patientId(1L)
                .build();
    }

    private UserSignUpDto userSignUpDto(){
        return UserSignUpDto.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .userId(1L)
                .userLoginIdentifier("detix123")
                .userName("김덴티")
                .userGender(GenderType.W)
                .build();
    }

    private UserLoginDto userLoginDto(){
        return UserLoginDto.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .userId(1L)
                .userName("김덴티")
                .build();
    }

    private UserFindPasswordDto userFindPasswordDto(){
        return UserFindPasswordDto.builder()
                .userId(1L)
                .userLoginIdentifier("detix123")
                .userName("김덴티")
                .build();
    }

    private AccessTokenDto accessTokenDto(){
        return AccessTokenDto.builder()
                .accessToken("AccessToken")
                .build();
    }


    /**
     *  사용자 회원 확인
     */
    @Test
    public void userVerify() throws Exception{

        // given
        given(userLoginService.userVerify(any(UserVerifyRequest.class))).willReturn(userVerifyDto());

        UserVerifyRequest userVerifyRequest = UserVerifyRequest.builder()
                .userPhoneNumber("01012345678")
                .userName("김덴티")
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login/verify")
                        .content(objectMapper.writeValueAsString(userVerifyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("login/verify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("patientPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("사용자(환자) 연락처"),
                                fieldWithPath("patientName").type(JsonFieldType.STRING).description("사용자(환자) 실명")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.patientId").type(JsonFieldType.NUMBER).optional().description("환자 고유 번호")
                        )
                ));

        verify(userLoginService).userVerify(any(UserVerifyRequest.class));
    }

    /**
     *  사용자 회원가입
     */
    @Test
    public void userSignUp() throws Exception{

        // given
        given(userLoginService.userSignUp(any(HttpServletRequest.class), any(UserSignUpRequest.class))).willReturn(userSignUpDto());

        List<Long> serviceAgreementList = Arrays.asList(1L, 2L, 3L);

        String password = "password!";
        UserSignUpRequest userSignUpRequest = UserSignUpRequest.builder()
                .userId(1L)
                .userServiceAgreementRequest(serviceAgreementList)
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPassword(password)
                .userGender(GenderType.W)
                .findPwdQuestionId(1L)
                .findPwdAnswer("초록색")
                .build();
        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login/signUp")
                        .content(objectMapper.writeValueAsString(userSignUpRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("login/signUp",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userId").type(JsonFieldType.NUMBER).optional().description("사용자(환자) 고유 번호"),
                                fieldWithPath("userServiceAgreementRequest[]").type(JsonFieldType.ARRAY).description("사용자 서비스 동의 고유 번호"),
                                fieldWithPath("userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("userName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("userPassword").type(JsonFieldType.STRING).description("사용자 비밀번호"),
                                fieldWithPath("userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별"),
                                fieldWithPath("findPwdQuestionId").type(JsonFieldType.NUMBER).description("사용자 비밀번호 찾기 질문"),
                                fieldWithPath("findPwdAnswer").type(JsonFieldType.STRING).description("사용자 비밀번호 찾기 답변")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("response.userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("response.userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")
                        )
                ));

        verify(userLoginService).userSignUp(any(HttpServletRequest.class), any(UserSignUpRequest.class));

    }

    /**
     *  아이디 중복 확인
     */
    @Test
    public void loginIdCheck() throws Exception{

        // given
        doNothing().when(userLoginService).loginIdCheck(any(String.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/login/loginIdentifier-check?userLoginIdentifier={userLoginIdentifier}", "dentix123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("login/loginIdentifier-check",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("userLoginIdentifier").description("사용자 아이디")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userLoginService).loginIdCheck(any(String.class));

    }

    /**
     *  사용자 로그인
     */
    @Test
    public void userLogin() throws Exception{

        // given
        given(userLoginService.userLogin(any(HttpServletRequest.class), any(UserLoginRequest.class))).willReturn(userLoginDto());

        String password = "password!";
        UserLoginRequest userLoginRequest = UserLoginRequest.builder()
                .userLoginIdentifier("dentix123")
                .userPassword(password)
                .build();
        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login")
                        .content(objectMapper.writeValueAsString(userLoginRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("login",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("userPassword").type(JsonFieldType.STRING).description("사용자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 이름")
                        )
                ));

        verify(userLoginService).userLogin(any(HttpServletRequest.class), any(UserLoginRequest.class));

    }

    /**
     *  사용자 비밀번호 찾기
     */
    @Test
    public void userFindPassword() throws Exception{

        // given
        given(userLoginService.userFindPassword(any(UserFindPasswordRequest.class))).willReturn(userFindPasswordDto());

        UserFindPasswordRequest userFindPasswordRequest = UserFindPasswordRequest.builder()
                .userLoginIdentifier("dentix123")
                .findPwdQuestionId(1L)
                .findPwdAnswer("초록색")
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login/find-password")
                        .content(objectMapper.writeValueAsString(userFindPasswordRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("login/find-password",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("findPwdQuestionId").type(JsonFieldType.NUMBER).description("사용자 비밀번호 찾기 질문"),
                                fieldWithPath("findPwdAnswer").type(JsonFieldType.STRING).description("사용자 비밀번호 찾기 답변")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("response.userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디")
                        )
                ));

        verify(userLoginService).userFindPassword(any(UserFindPasswordRequest.class));

    }

    /**
     *  사용자 비밀번호 재설정
     */
    @Test
    public void modifyPassword() throws Exception{

        // given
        doNothing().when(userLoginService).userModifyPassword(any(UserModifyPasswordRequest.class));

        String password = "password!";
        UserModifyPasswordRequest userPasswordVerifyRequest = UserModifyPasswordRequest.builder()
                .userId(1L)
                .userPassword(password)
                .build();
        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/login/password")
                        .content(objectMapper.writeValueAsString(userPasswordVerifyRequest))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("login/password",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("userPassword").type(JsonFieldType.STRING).description("사용자 비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userLoginService).userModifyPassword(any(UserModifyPasswordRequest.class));
    }

    /**
     *  AccessToken 재발급
     */
    @Test
    public void accessTokenReissue() throws Exception{

        // given
        given(userLoginService.accessTokenReissue(any(HttpServletRequest.class))).willReturn(accessTokenDto());

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/login/access-token")
                        .header("RefreshToken", "access-token.고유경.RefreshToken")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("login/access-token",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token")
                        )
                ));
    }

}