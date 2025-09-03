//package com.kaii.dentix.domain.admin;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.kaii.dentix.common.ControllerTest;
//import com.kaii.dentix.domain.admin.application.AdminPatientService;
//import com.kaii.dentix.domain.admin.controller.AdminPatientController;
//import com.kaii.dentix.domain.admin.dto.*;
//import com.kaii.dentix.domain.admin.dto.request.AdminPatientListRequest;
//import com.kaii.dentix.domain.admin.dto.request.AdminRegisterPatientRequest;
//import com.kaii.dentix.domain.type.YnType;
//import com.kaii.dentix.global.common.dto.PagingDTO;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.restdocs.RestDocumentationContextProvider;
//import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
//import org.springframework.restdocs.payload.JsonFieldType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.web.context.WebApplicationContext;
//
//import java.util.ArrayList;
//import java.util.Date;
//
//import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
//import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
//import static com.kaii.dentix.common.DocumentOptionalGenerator.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.verify;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
//import static org.springframework.restdocs.payload.PayloadDocumentation.*;
//import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
//import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
//import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(AdminPatientController.class)
//public class AdminPatientControllerTest extends ControllerTest {
//
//    private MockMvc mockMvc;
//
//    @BeforeEach
//    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
//        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
//                .apply(documentationConfiguration(restDocumentation))
//                .build();
//    }
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private AdminPatientService adminPatientService;
//
//    public AdminRegisterPatientDto adminRegisterPatientDto(){
//        return AdminRegisterPatientDto.builder()
//                .patientId(10L)
//                .build();
//    }
//
//    /**
//     *  관리자 환자 등록
//     */
//    @Test
//    public void adminRegisterPatient() throws Exception {
//
//        // given
//        given(adminPatientService.adminRegisterPatient(any(AdminRegisterPatientRequest.class))).willReturn(adminRegisterPatientDto());
//
//        AdminRegisterPatientRequest adminRegisterPatientRequest = AdminRegisterPatientRequest.builder()
//                .patientName("김환자")
//                .patientPhoneNumber("01098989898")
//                .build();
//
//        // when
//        ResultActions resultActions = mockMvc.perform(
//                RestDocumentationRequestBuilders.post("/admin/patient")
//                        .content(objectMapper.writeValueAsString(adminRegisterPatientRequest))
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION, "admin-patient.고유경.AccessToken")
//                        .with(user("user").roles("ADMIN"))
//        );
//
//        // then
//        resultActions.andExpect(status().isOk())
//                .andExpect(jsonPath("rt").value(200))
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andDo(document("admin/patient/register",
//                        getDocumentRequest(),
//                        getDocumentResponse(),
//                        requestFields(
//                                fieldWithPath("patientName").type(JsonFieldType.STRING).description("환자 이름"),
//                                fieldWithPath("patientPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("환자 연락처")
//                        ),
//                        responseFields(
//                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
//                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
//                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
//                                fieldWithPath("response.patientId").type(JsonFieldType.NUMBER).description("환자 고유 번호")
//                        )
//                ));
//
//        verify(adminPatientService).adminRegisterPatient(any(AdminRegisterPatientRequest.class));
//    }
//
//    /**
//     *  관리자 환자 목록 조회
//     */
//    @Test
//    public void adminPatientList() throws Exception {
//        Date date = new Date();
//
//        // given
//        AdminPatientListDto adminPatientList = AdminPatientListDto.builder()
//                .paging(new PagingDTO(1, 2, 15))
//                .patientList(new ArrayList<>(){{
//                    add(new AdminPatientInfoDto(1L, "김덴티", "01012345678", date, YnType.Y));
//                    add(new AdminPatientInfoDto(2L, "홍길동", "01098765432", date, YnType.N));
//                }})
//                .build();
//
//        given(adminPatientService.adminPatientList(any(AdminPatientListRequest.class))).willReturn(adminPatientList);
//
//        // when
//        ResultActions result = mockMvc.perform(
//                RestDocumentationRequestBuilders.get("/admin/patient?page={page}&size={size}", 1, 10)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION, "admin-patient-list.고유경.AccessToken")
//                        .with(user("user").roles("ADMIN"))
//        );
//
//        // then
//        result.andExpect(status().isOk())
//                .andExpect(jsonPath("rt").value(200))
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andDo(document("admin/patient/list",
//                        getDocumentRequest(),
//                        getDocumentResponse(),
//                        queryParameters(
//                                parameterWithName("page").description("요청 페이지"),
//                                parameterWithName("size").description("한 페이지에 가져올 목록 개수"),
//                                parameterWithName("patientNameOrPhoneNumber").optional().description("환자 이름 혹은 연락처")
//                        ),
//                        responseFields(
//                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
//                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
//                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
//                                fieldWithPath("response.paging").type(JsonFieldType.OBJECT).description("페이징 정보"),
//                                fieldWithPath("response.paging.number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
//                                fieldWithPath("response.paging.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 개수"),
//                                fieldWithPath("response.paging.totalElements").type(JsonFieldType.NUMBER).description("총 데이터 개수"),
//                                fieldWithPath("response.patientList[]").type(JsonFieldType.ARRAY).description("환자 목록"),
//                                fieldWithPath("response.patientList[].patientId").type(JsonFieldType.NUMBER).description("환자 고유 번호"),
//                                fieldWithPath("response.patientList[].patientName").type(JsonFieldType.STRING).description("환자 이름"),
//                                fieldWithPath("response.patientList[].patientPhoneNumber").type(JsonFieldType.STRING).attributes(userNumberFormat()).description("환자 연락처"),
//                                fieldWithPath("response.patientList[].created").type(JsonFieldType.STRING).attributes(dateFormat()).description("환자 등록일"),
//                                fieldWithPath("response.patientList[].isUser").type(JsonFieldType.STRING).attributes(yesNoFormat()).description("환자 가입 여부")
//                        )
//                ));
//
//        verify(adminPatientService).adminPatientList(any(AdminPatientListRequest.class));
//    }
//
//    /**
//     *  관리자 환자 삭제
//     */
//    @Test
//    public void adminDeletePatient() throws Exception{
//        // given
//        doNothing().when(adminPatientService).adminDeletePatient(any(Long.class));
//
//        // when
//        ResultActions resultActions = mockMvc.perform(
//                RestDocumentationRequestBuilders.delete("/admin/patient?patientId={patientId}", 1)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header(HttpHeaders.AUTHORIZATION, "patient-delete.고유경.AccessToken")
//                        .with(user("user").roles("ADMIN"))
//        );
//
//        // then
//        resultActions.andExpect(status().isOk())
//                .andExpect(jsonPath("rt").value(200))
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andDo(document("admin/patient/delete",
//                        getDocumentRequest(),
//                        getDocumentResponse(),
//                        queryParameters(
//                                parameterWithName("patientId").description("환자 고유 번호")
//                        ),
//                        responseFields(
//                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
//                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지")
//                        )
//                ));
//
//        verify(adminPatientService).adminDeletePatient(any(Long.class));
//    }
//
//}
