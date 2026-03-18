package com.kaii.dentix.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.agreement.application.ServiceAgreementConsentService;
import com.kaii.dentix.domain.agreement.dto.ServiceAgreementConsentDto;
import com.kaii.dentix.domain.toothBrushing.application.ToothBrushingService;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRegisterDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRequestDto;
import com.kaii.dentix.domain.type.GenderType;
import com.kaii.dentix.domain.type.ServiceType;
import com.kaii.dentix.domain.type.YnType;
import com.kaii.dentix.domain.user.application.UserService;
import com.kaii.dentix.domain.user.controller.UserController;
import com.kaii.dentix.domain.user.dto.UserDto;
import com.kaii.dentix.domain.user.dto.UserLoginDto;
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
@ExtendWith(RestDocumentationExtension.class)
public class UserControllerTest {

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

    @MockBean
    private ToothBrushingService toothBrushingService;

    @MockBean
    private ServiceAgreementConsentService serviceAgreementConsentService;

    private UserLoginDto userLoginDto(){
        return UserLoginDto.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .userId(1L)
                .userName("김덴티")
                .build();
    }

    private UserDto.TokenResponse tokenResponse() {
        return UserDto.TokenResponse.builder()
                .accessToken("Access Token")
                .refreshToken("Refresh Token")
                .build();
    }

    private UserDto.InfoResponse userInfoResponse() {
        return UserDto.InfoResponse.builder()
                .userName("강덴티") // 혹은 "김덴티"
                .userLoginIdentifier("dentix123")
                .userGender(GenderType.W)
                .services(List.of(
                        new UserDto.ServiceInfo(1L, "구강 검진", ServiceType.PLAQUE_DETECTION)
                ))
                .build();
    }

    private ServiceAgreementConsentDto.ModifyResponse userModifyServiceAgreeDto(){
        Date date = new Date();
        return ServiceAgreementConsentDto.ModifyResponse.builder()
                .serviceAgreeId(3L)
                .isUserServiceAgree(YnType.Y)
                .date(date)
                .build();
    }
    private UserDto.ModifyQnAResponse modifyQnAResponse() {
        return UserDto.ModifyQnAResponse.builder()
                .findPwdQuestionId(1L)
                .findPwdAnswer("수정된 답변")
                .build();
    }

    private UserDto.ServiceUpdateResponse serviceUpdateResponse() {
        return UserDto.ServiceUpdateResponse.builder()
                .userName("강덴티")
                .services(List.of(
                        new UserDto.ServiceInfo(1L, "구강 검진", ServiceType.PLAQUE_DETECTION),
                        new UserDto.ServiceInfo(2L, "치주 진단", ServiceType.PERIODONTAL_DETECTION)
                ))
                .build();
    }

    private ToothBrushingRegisterDto toothBrushingRegisterResponse() {
        return ToothBrushingRegisterDto.builder()
                .toothBrushingList(List.of(new ToothBrushingDto(1L, new Date())))
                .build();
    }


    /**
     *  사용자 자동 로그인
     */
    @Test
    public void userAutoLogin() throws Exception{

        // given
        given(userService.userAutoLogin(any(HttpServletRequest.class))).willReturn(tokenResponse());

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user/auto-login")
                        .accept(MediaType.APPLICATION_JSON)
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
                                fieldWithPath("response.refreshToken").type(JsonFieldType.STRING).description("Refresh Token")
                        )
                ));

        verify(userService).userAutoLogin(any(HttpServletRequest.class));

    }

    /**
     *  사용자 비밀번호 확인
     */
    @Test
    public void userPasswordVerify() throws Exception{

        // given
        doNothing().when(userService).userPasswordVerify(any(HttpServletRequest.class), any(UserDto.PasswordVerifyRequest.class));

        String password = "password";
        UserDto.PasswordVerifyRequest userPasswordVerifyRequest = UserDto.PasswordVerifyRequest.builder()
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

        verify(userService).userPasswordVerify(any(HttpServletRequest.class), any(UserDto.PasswordVerifyRequest.class));

    }

    /**
     *  사용자 보안정보수정 - 비밀번호 변경
     */
    @Test
    public void userModifyPassword() throws Exception{

        // given
        doNothing().when(userService).userModifyPassword(any(HttpServletRequest.class), any(UserDto.ModifyPasswordRequest.class));

        String password = "password!";
        UserDto.ModifyPasswordRequest userInfoModifyPasswordRequest = UserDto.ModifyPasswordRequest.builder()
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

        verify(userService).userModifyPassword(any(HttpServletRequest.class), any(UserDto.ModifyPasswordRequest.class));

    }

    /**
     *  사용자 보안정보수정 - 질문과 답변 수정
     */
    @Test
    public void userModifyQnA() throws Exception {


        // given
        given(userService.userModifyQnA(any(HttpServletRequest.class), any(UserDto.ModifyQnARequest.class)))
                .willReturn(modifyQnAResponse());
        UserDto.ModifyQnARequest userInfoModifyQnARequest = UserDto.ModifyQnARequest.builder()
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

        verify(userService).userModifyQnA(any(HttpServletRequest.class), any(UserDto.ModifyQnARequest.class));

    }
    /**
     * 사용자 회원정보 수정
     */
    @Test
    public void userModifyInfo() throws Exception{

        // given
        given(userService.userModifyInfo(any(HttpServletRequest.class), any(UserDto.ModifyInfoRequest.class)))
                .willReturn(userInfoResponse());

        UserDto.ModifyInfoRequest requestDto = UserDto.ModifyInfoRequest.builder()
                .userName("강덴티")
                .userGender(GenderType.W)
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.put("/user")
                        .content(objectMapper.writeValueAsString(requestDto)) // [수정] requestDto 사용
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/modify-info", // 문서명 구분을 위해 이름 변경 추천 (선택)
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("userName").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별")
                        ),
                        // [수정] userInfoResponse()에 맞춰서 responseFields 보완 (에러 방지)
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 닉네임"),
                                fieldWithPath("response.userLoginIdentifier").type(JsonFieldType.STRING).description("사용자 아이디"),
                                fieldWithPath("response.userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별"),
                                fieldWithPath("response.services").type(JsonFieldType.ARRAY).optional().description("이용 중인 서비스 목록"),
                                fieldWithPath("response.services[].serviceId").type(JsonFieldType.NUMBER).optional().description("서비스 고유 번호"),
                                fieldWithPath("response.services[].name").type(JsonFieldType.STRING).optional().description("서비스 이름"),
                                fieldWithPath("response.services[].serviceType").type(JsonFieldType.STRING).optional().description("서비스 타입")
                        )
                ));
        verify(userService).userModifyInfo(any(HttpServletRequest.class), any(UserDto.ModifyInfoRequest.class));

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
        given(userService.updateUserServices(any(HttpServletRequest.class), any(UserDto.ServiceUpdateRequest.class)))
                .willReturn(serviceUpdateResponse());

        UserDto.ServiceUpdateRequest userModifyServiceAgreeRequest = UserDto.ServiceUpdateRequest.builder()
                .serviceIds(List.of(1L, 2L))
                .build();

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/user/service")
                        .content(objectMapper.writeValueAsString(userModifyServiceAgreeRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/service",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("serviceIds").type(JsonFieldType.ARRAY).description("선택한 서비스 ID 목록")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.userName").type(JsonFieldType.STRING).description("사용자 이름"),
                                fieldWithPath("response.services").type(JsonFieldType.ARRAY).description("선택된 서비스 목록"),
                                fieldWithPath("response.services[].serviceId").type(JsonFieldType.NUMBER).description("서비스 ID"),
                                fieldWithPath("response.services[].name").type(JsonFieldType.STRING).description("서비스 이름"),
                                fieldWithPath("response.services[].serviceType").type(JsonFieldType.STRING).description("서비스 타입")
                        )
                ));

        verify(userService).updateUserServices(any(HttpServletRequest.class), any(UserDto.ServiceUpdateRequest.class));

    }

    @Test
    public void recordToothBrushing() throws Exception{

        // given
        ToothBrushingRequestDto requestDto = ToothBrushingRequestDto.builder()
                .brushingTime("09:30")
                .build();

        given(toothBrushingService.recordToothBrushing(any(HttpServletRequest.class), any(ToothBrushingRequestDto.class)))
                .willReturn(toothBrushingRegisterResponse());

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.post("/user/brushing")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "user-info.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("user/brushing",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        requestFields(
                                fieldWithPath("brushingTime").type(JsonFieldType.STRING).description("양치 시간(HH:mm 또는 HH:mm:ss)")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.toothBrushingList").type(JsonFieldType.ARRAY).description("해당 일자 양치 기록 목록"),
                                fieldWithPath("response.toothBrushingList[].toothBrushingId").type(JsonFieldType.NUMBER).description("양치 기록 ID"),
                                fieldWithPath("response.toothBrushingList[].created").type(JsonFieldType.STRING).attributes(dateTimeFormat()).description("양치 기록 시각"),
                                fieldWithPath("response.timeInterval").type(JsonFieldType.NUMBER).optional().attributes(timeIntervalFormat()).description("다음 기록 가능까지 남은 시간(초)")
                        )
                ));

        verify(toothBrushingService).recordToothBrushing(any(HttpServletRequest.class), any(ToothBrushingRequestDto.class));

    }

    /**
     *  사용자 회원정보 조회
     */
    @Test
    public void userInfo() throws Exception{

        // given
        given(userService.getUserInfo(any(HttpServletRequest.class))).willReturn(userInfoResponse());
        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/user/info")
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
                                fieldWithPath("response.userGender").type(JsonFieldType.STRING).optional().attributes(genderFormat()).description("사용자 성별"),
                                fieldWithPath("response.services").type(JsonFieldType.ARRAY).optional().description("이용 중인 서비스 목록"),
                                fieldWithPath("response.services[].serviceId").type(JsonFieldType.NUMBER).optional().description("서비스 고유 번호"),
                                fieldWithPath("response.services[].name").type(JsonFieldType.STRING).optional().description("서비스 이름"),
                                fieldWithPath("response.services[].serviceType").type(JsonFieldType.STRING).optional().description("서비스 타입")
                        )
                ));
        verify(userService).getUserInfo(any(HttpServletRequest.class));
    }

}
