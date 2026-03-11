package com.kaii.dentix.domain.toothBrushingController;

import com.kaii.dentix.domain.toothBrushing.application.ToothBrushingService;
import com.kaii.dentix.domain.toothBrushing.controller.ToothBrushingController;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingDto;
import com.kaii.dentix.domain.toothBrushing.dto.ToothBrushingRegisterDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static com.kaii.dentix.common.DocumentOptionalGenerator.dateTimeFormat;
import static com.kaii.dentix.common.DocumentOptionalGenerator.timeIntervalFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToothBrushingController.class)
@ExtendWith(RestDocumentationExtension.class)
public class ToothBrushingControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @MockBean
    private ToothBrushingService toothBrushingService;

    /**
     * 양치질 기록
     */
    @Test
    public void toothBrushing() throws Exception{
        ToothBrushingRegisterDto toothBrushingRegisterDto = new ToothBrushingRegisterDto(List.of(new ToothBrushingDto(1L, new Date())), null);

        // given
        given(toothBrushingService.toothBrushing(any(HttpServletRequest.class))).willReturn(toothBrushingRegisterDto);

        // when
        ResultActions resultActions = mockMvc.perform(
            RestDocumentationRequestBuilders.post("/toothBrushing")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "toothBrushing.이호준.AccessToken")
                .with(user("user").roles("USER"))
        );

        // then
        resultActions.andExpect(status().isOk())
            .andExpect(jsonPath("rt").value(200))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andDo(document("toothBrushing",
                getDocumentRequest(),
                getDocumentResponse(),
                responseFields(
                    fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                    fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                    fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                    fieldWithPath("response.toothBrushingList").type(JsonFieldType.ARRAY).description("금일 양치질 목록 (양치 실패 시 null)"),
                    fieldWithPath("response.toothBrushingList[].toothBrushingId").type(JsonFieldType.NUMBER).description("양치질 고유번호"),
                    fieldWithPath("response.toothBrushingList[].created").type(JsonFieldType.STRING).attributes(dateTimeFormat()).description("양치질 시각"),
                    fieldWithPath("response.timeInterval").type(JsonFieldType.NUMBER).optional().attributes(timeIntervalFormat()).description("양치 실패 시 다음 양치 가능까지 남은 시각 (양치 성공 시 null)")
                )
            ));

        verify(toothBrushingService).toothBrushing(any(HttpServletRequest.class));
    }

}
