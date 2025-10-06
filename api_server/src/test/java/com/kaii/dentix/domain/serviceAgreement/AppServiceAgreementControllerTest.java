package com.kaii.dentix.domain.serviceAgreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.common.ControllerTest;
import com.kaii.dentix.domain.serviceAgreement.application.ServiceAgreementService;
import com.kaii.dentix.domain.serviceAgreement.controller.ServiceAgreementController;
import com.kaii.dentix.domain.serviceAgreement.dto.ServiceAgreementDto;
import com.kaii.dentix.domain.serviceAgreement.dto.ServiceAgreementListDto;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentRequest;
import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.isRequiredFormat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceAgreementController.class)
public class AppServiceAgreementControllerTest extends ControllerTest {

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
    private ServiceAgreementService serviceAgreementService;

    /**
     *  약관 전체 조회
     */
    @Test
    public void serviceAgreement() throws Exception{

        // given
        List<ServiceAgreementDto> serviceAgreementList = Arrays.asList(
                ServiceAgreementDto.builder()
                        .id(1L)
                        .name("서비스 이용약관 동의")
                        .menuName("서비스 이용약관")
                        .isServiceAgreeRequired(YnType.Y)
                        .path("https://dthi-dev.kai-i.com/")
                        .build(),
                ServiceAgreementDto.builder()
                        .id(2L)
                        .name("개인정보 수집 및 이용 동의")
                        .menuName("개인정보 수집 및 이용")
                        .isServiceAgreeRequired(YnType.Y)
                        .path("https://dtroka-dev.kai-i.com/")
                        .build(),
                ServiceAgreementDto.builder()
                        .id(3L)
                        .name("마케팅 정보 수신 동의")
                        .menuName("마케팅 정보 수신")
                        .isServiceAgreeRequired(YnType.N)
                        .path("https://dentix-api-dev.kai-i.com/docs/app-api-guide.html")
                        .build()
        );

        given(serviceAgreementService.serviceAgreementList()).willReturn(new ServiceAgreementListDto(serviceAgreementList));

        // when
        ResultActions resultActions = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/service-agreement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("service-agreement",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.serviceAgreement").type(JsonFieldType.ARRAY).description("서비스 동의 목록"),
                                fieldWithPath("response.serviceAgreement[].id").type(JsonFieldType.NUMBER).description("서비스 동의 고유 번호"),
                                fieldWithPath("response.serviceAgreement[].name").type(JsonFieldType.STRING).description("서비스 동의 이름"),
                                fieldWithPath("response.serviceAgreement[].menuName").type(JsonFieldType.STRING).description("서비스 동의 메뉴 이름"),
                                fieldWithPath("response.serviceAgreement[].isServiceAgreeRequired").type(JsonFieldType.STRING).attributes(isRequiredFormat()).description("서비스 동의 필수 여부"),
                                fieldWithPath("response.serviceAgreement[].path").type(JsonFieldType.STRING).description("서비스 동의 경로")
                                )
                ));

        verify(serviceAgreementService).serviceAgreementList();

    }

}