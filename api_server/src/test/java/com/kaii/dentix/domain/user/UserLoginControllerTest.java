package com.kaii.dentix.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.admin.application.AdminLoginService;
import com.kaii.dentix.domain.auth.controller.AuthController;
import com.kaii.dentix.global.common.error.exception.UnauthorizedException;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.user.application.UserLoginService;
import com.kaii.dentix.domain.user.domain.UserDaeguCredentialStatus;
import com.kaii.dentix.domain.user.domain.UserDaeguIdentityStatus;
import com.kaii.dentix.domain.user.controller.UserLoginController;
import com.kaii.dentix.domain.user.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
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


@WebMvcTest({UserLoginController.class, AuthController.class})
@ExtendWith(RestDocumentationExtension.class)
public class UserLoginControllerTest {

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

    @MockBean
    private AdminLoginService adminLoginService;

    // =================================================================
    // Helper Methods (UserDto 사용)
    // =================================================================

    private UserDto.VerifyResponse userVerifyDto() {
        return UserDto.VerifyResponse.builder()
                .userId(1L)
                .build();
    }

    private UserDto.SignUpResponse userSignUpDto() {
        return UserDto.SignUpResponse.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .userId(1L)
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userGender(GenderType.W)
                .organizationId(1L)
                .organizationName("테스트 치과")
                .daeguDid("did:mitum:minic:0x123")
                .daeguDidStatus(UserDaeguIdentityStatus.ISSUED)
                .daeguCredentialStatus(UserDaeguCredentialStatus.ISSUED)
                .build();
    }

    private UserDto.LoginResponse userLoginDto() {
        return UserDto.LoginResponse.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .userId(1L)
                .userName("김덴티")
                .serviceId(1L)
                .name("메인 서비스")
                .services(List.of())
                .organizationId(1L)
                .organizationName("테스트 치과")
                .organizationPlanName("GROWTH")
                .organizationCustomSurveyEnabled(true)
                .build();
    }

    private UserDto.AccessTokenResponse accessTokenDto() {
        return UserDto.AccessTokenResponse.builder()
                .accessToken("New Access Token")
                .build();
    }

    // =================================================================
    // Tests
    // =================================================================

    /**
     * 사용자 회원 확인
     */
    @Test
    public void userVerify() throws Exception {
        // given
        given(userLoginService.userVerify(any(UserDto.VerifyRequest.class))).willReturn(userVerifyDto());

        UserDto.VerifyRequest request = UserDto.VerifyRequest.builder()
                .userPhoneNumber("010-1234-5678")
                .userName("김덴티")
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login/verify")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("login/verify",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("사용자 휴대폰 번호"),
                                fieldWithPath("userName").type(JsonFieldType.STRING).description("사용자 이름")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).optional().description("사용자 고유 번호 (이미 가입된 경우)")
                        )
                ));

        verify(userLoginService).userVerify(any(UserDto.VerifyRequest.class));
    }

    /**
     * 사용자 회원가입
     */
    @Test
    public void userSignUp() throws Exception {
        // given
        // Controller에서 HttpServletRequest를 받지 않으므로 matcher 제거
        given(userLoginService.userSignUp(any(UserDto.SignUpRequest.class))).willReturn(userSignUpDto());

        String password = "password!";
        UserDto.SignUpRequest request = UserDto.SignUpRequest.builder()
                .userLoginIdentifier("dentix123")
                .userName("김덴티")
                .userPassword(password)
                .userGender(GenderType.W)
                .userPhoneNumber("01012345678")
                .findPwdQuestionId(1L)
                .findPwdAnswer("초록색")
                .organizationId(1L)
                .userServiceAgreementRequest(Arrays.asList(1L, 2L, 3L))
                .build();

        given(passwordEncoder.encode(any(String.class))).willReturn(password);

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login/signUp")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("login/signUp",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("userName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("userPassword").type(JsonFieldType.STRING).description("사용자 비밀번호"),
                                fieldWithPath("userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별"),
                                fieldWithPath("userPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("사용자 휴대폰 번호"),
                                fieldWithPath("findPwdQuestionId").type(JsonFieldType.NUMBER).description("비밀번호 찾기 질문 ID"),
                                fieldWithPath("findPwdAnswer").type(JsonFieldType.STRING).description("비밀번호 찾기 답변"),
                                fieldWithPath("organizationId").type(JsonFieldType.NUMBER).description("소속 기관 ID"),
                                fieldWithPath("userServiceAgreementRequest").type(JsonFieldType.ARRAY).description("동의한 약관 ID 목록")
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
                                fieldWithPath("response.userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별"),
                                fieldWithPath("response.organizationId").type(JsonFieldType.NUMBER).description("소속 기관 ID"),
                                fieldWithPath("response.organizationName").type(JsonFieldType.STRING).description("소속 기관 이름"),
                                fieldWithPath("response.daeguDid").type(JsonFieldType.STRING).optional().description("대구 DID"),
                                fieldWithPath("response.daeguDidStatus").type(JsonFieldType.STRING).optional().description("대구 DID 발급 상태"),
                                fieldWithPath("response.daeguCredentialStatus").type(JsonFieldType.STRING).optional().description("대구 DID credential 발급 상태")
                        )
                ));

        verify(userLoginService).userSignUp(any(UserDto.SignUpRequest.class));
    }

    /**
     * 아이디 중복 확인
     */
    @Test
    public void loginIdCheck() throws Exception {
        // given
        doNothing().when(userLoginService).loginIdCheck(any(String.class));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/login/loginIdentifier-check")
                        .param("userLoginIdentifier", "dentix123")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("login/loginIdentifier-check",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("userLoginIdentifier").description("중복 확인할 사용자 아이디")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
                        )
                ));

        verify(userLoginService).loginIdCheck(any(String.class));
    }

    /**
     * 사용자 로그인
     */
    @Test
    public void userLogin() throws Exception {
        given(userLoginService.userLogin(any(UserDto.LoginRequest.class))).willReturn(userLoginDto());

        String request = """
                {
                  "userType": "user",
                  "loginId": "dentix123",
                  "password": "password!"
                }
                """;

        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andDo(document("login",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userType").type(JsonFieldType.STRING).description("로그인 사용자 타입"),
                                fieldWithPath("loginId").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("password").type(JsonFieldType.STRING).optional().description("관리자 비밀번호. 사용자 로그인은 비밀번호 없이 요청 가능")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userId").type(JsonFieldType.NUMBER).description("사용자 고유 번호"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token"),
                                fieldWithPath("response.serviceId").type(JsonFieldType.NUMBER).optional().description("대표 서비스 ID"),
                                fieldWithPath("response.name").type(JsonFieldType.STRING).optional().description("대표 서비스 이름"),
                                fieldWithPath("response.services").type(JsonFieldType.ARRAY).optional().description("이용 가능한 서비스 목록"),
                                fieldWithPath("response.organizationId").type(JsonFieldType.NUMBER).optional().description("소속 기관 ID"),
                                fieldWithPath("response.organizationName").type(JsonFieldType.STRING).optional().description("소속 기관 이름"),
                                fieldWithPath("response.organizationPlanName").type(JsonFieldType.STRING).optional().description("기관 구독 플랜 이름"),
                                fieldWithPath("response.organizationCustomSurveyEnabled").type(JsonFieldType.BOOLEAN).optional().description("커스텀 설문 기능 사용 가능 여부"),
                                fieldWithPath("response.daeguDid").type(JsonFieldType.STRING).optional().description("DaeguChain DID"),
                                fieldWithPath("response.daeguDidStatus").type(JsonFieldType.STRING).optional().description("DaeguChain DID status")
                        )
                ));

        verify(userLoginService).userLogin(any(UserDto.LoginRequest.class));
    }

    @Test
    public void userLoginAllowsPasswordlessUserRequest() throws Exception {
        given(userLoginService.userLogin(any(UserDto.LoginRequest.class))).willReturn(userLoginDto());

        String request = """
                {
                  "userType": "user",
                  "loginId": "dentix123"
                }
                """;

        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/login")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200));

        verify(userLoginService).userLogin(any(UserDto.LoginRequest.class));
    }

    @Test
    public void userLoginDoesNotExposePasswordCheckMessage() throws Exception {
        given(userLoginService.userLogin(any(UserDto.LoginRequest.class)))
                .willThrow(new UnauthorizedException("Invalid login identifier or password."));

        String request = """
                {
                  "userType": "user",
                  "loginId": "dentix123",
                  "password": "password!"
                }
                """;

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/login")
                                .header("Accept-Language", "en-US")
                                .content(request)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(401))
                .andExpect(jsonPath("rtMsg").value("DID credential verification failed."));
    }

    @Test
    public void userLoginRequiresAdminApprovalMessageInKorean() throws Exception {
        given(userLoginService.userLogin(any(UserDto.LoginRequest.class)))
                .willThrow(new UnauthorizedException("관리자 승인을 받아야 로그인할 수 있습니다."));

        String request = """
                {
                  "userType": "user",
                  "loginId": "dentix123",
                  "password": "password!"
                }
                """;

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/login")
                                .content(request)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(401))
                .andExpect(jsonPath("rtMsg").value("관리자 승인을 받아야 로그인할 수 있습니다."));
    }

    @Test
    public void userLoginRequiresAdminApprovalMessageInEnglish() throws Exception {
        given(userLoginService.userLogin(any(UserDto.LoginRequest.class)))
                .willThrow(new UnauthorizedException("관리자 승인을 받아야 로그인할 수 있습니다."));

        String request = """
                {
                  "userType": "user",
                  "loginId": "dentix123",
                  "password": "password!"
                }
                """;

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/login")
                                .header("Accept-Language", "en-US")
                                .content(request)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(401))
                .andExpect(jsonPath("rtMsg").value("Administrator approval is required before you can log in."));
    }

    /**
     * AccessToken 재발급
     */
    @Test
    public void accessTokenReissue() throws Exception {
        // given
        given(userLoginService.accessTokenReissue(any(HttpServletRequest.class))).willReturn(accessTokenDto());

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/login/access-token")
                        .header("RefreshToken", "access-token.refresh.token")
                        .accept(MediaType.APPLICATION_JSON)
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
                                fieldWithPath("response.accessToken").type(JsonFieldType.STRING).description("새로 발급된 Access Token"),
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).optional().description("Refresh Token (변경 시)")
                        )
                ));
    }
}
