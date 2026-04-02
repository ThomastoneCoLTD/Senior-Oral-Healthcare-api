package com.kaii.dentix.domain.contents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.contents.application.ContentsService;
import com.kaii.dentix.domain.contents.controller.ContentsController;
import com.kaii.dentix.domain.contents.dto.*;
import com.kaii.dentix.domain.type.ContentsType;
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

import java.util.Arrays;
import java.util.List;

import static com.kaii.dentix.common.ApiDocumentUtils.getDocumentResponse;
import static com.kaii.dentix.common.DocumentOptionalGenerator.contentsTypeFormat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentsController.class)
@ExtendWith(RestDocumentationExtension.class)
public class ContentsControllerTest {

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
    private ContentsService contentsService;

    @MockBean
    private com.kaii.dentix.domain.user.application.UserService userService;

    /**
     *  콘텐츠 조회
     */
    @Test
    public void contentsList() throws Exception{

        // given
        List<ContentsDto.Category> categories = Arrays.asList(
                ContentsDto.Category.builder()
                        .id(0)
                        .name("김덴티님 맞춤")
                        .color(null)
                        .sort(0)
                        .build(),
                ContentsDto.Category.builder()
                        .id(1)
                        .name("질병")
                        .color("#98B4ED")
                        .sort(1)
                        .build(),
                ContentsDto.Category.builder()
                        .id(2)
                        .name("양치")
                        .color("#4B79EC")
                        .sort(2)
                        .build()
        );

        List<Integer> contentsLists = Arrays.asList(1, 2);

        List<ContentsDto.Summary> contents = Arrays.asList(
                ContentsDto.Summary.builder()
                        .id(1L)
                        .sort(1)
                        .title("백살도 거뜬한 건강한 치아관리 방법")
                        .type(ContentsType.CARD)
                        .typeColor("#FF9F06")
                        .thumbnail("https://dentix-api-dev.kai-i.com")
                        .videoURL(null)
                        .categoryIds(contentsLists)
                        .build()
        );


        given(contentsService.getContentsList(any()))
                .willReturn(ContentsDto.ListResponse.builder()
                        .menuTabs(List.of(
                                ContentsDto.MenuTab.builder().id("PERSONALIZED").name("김덴티님 맞춤").build(),
                                ContentsDto.MenuTab.builder().id("ALL").name("모든 콘텐츠").build()
                        ))
                        .categories(categories)   // 아래에서 categories 타입도 바꿀 거야
                        .contents(contents)
                        .build());
        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/contents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "contents-list.고유경.AccessToken")
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("contents",
                        getDocumentResponse(),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.menuTabs").type(JsonFieldType.ARRAY).description("헤더 메뉴 탭 목록"),
                                fieldWithPath("response.menuTabs[].id").type(JsonFieldType.STRING).description("헤더 메뉴 탭 ID"),
                                fieldWithPath("response.menuTabs[].name").type(JsonFieldType.STRING).description("헤더 메뉴 탭 이름"),
                                fieldWithPath("response.categories").type(JsonFieldType.ARRAY).description("콘텐츠 카테고리 목록"),
                                fieldWithPath("response.categories[].id").type(JsonFieldType.NUMBER).description("콘텐츠 카테고리 고유 번호"),
                                fieldWithPath("response.categories[].name").type(JsonFieldType.STRING).description("콘텐츠 카테고리 이름"),
                                fieldWithPath("response.categories[].color").type(JsonFieldType.STRING).optional().description("콘텐츠 카테고리 색상"),
                                fieldWithPath("response.categories[].sort").type(JsonFieldType.NUMBER).description("콘텐츠 카테고리 정렬 순서"),
                                fieldWithPath("response.contents").type(JsonFieldType.ARRAY).description("콘텐츠 목록"),
                                fieldWithPath("response.contents[].id").type(JsonFieldType.NUMBER).description("콘텐츠 고유 번호"),
                                fieldWithPath("response.contents[].title").type(JsonFieldType.STRING).description("콘텐츠 제목"),
                                fieldWithPath("response.contents[].sort").type(JsonFieldType.NUMBER).description("콘텐츠 정렬 순서"),
                                fieldWithPath("response.contents[].type").type(JsonFieldType.STRING).attributes(contentsTypeFormat()).description("콘텐츠 타입"),
                                fieldWithPath("response.contents[].typeColor").type(JsonFieldType.STRING).description("콘텐츠 제목 색상"),
                                fieldWithPath("response.contents[].thumbnail").type(JsonFieldType.STRING).description("콘텐츠 썸네일"),
                                fieldWithPath("response.contents[].videoURL").type(JsonFieldType.STRING).optional().description("콘텐츠 동영상 경로"),
                                fieldWithPath("response.contents[].categoryIds").type(JsonFieldType.ARRAY).description("콘텐츠 카테고리"),
                                fieldWithPath("response.contents[].categoryIds[]").type(JsonFieldType.ARRAY).description("콘텐츠 카테고리 고유 번호")
                        )
                ));

        verify(contentsService).getContentsList(any());

    }

    /**
     *  콘텐츠 카드 뉴스 조회
     */
    @Test
    public void contentsCard() throws Exception{

        // given
        List<ContentsCardDto> contentsCardList = Arrays.asList(
                ContentsCardDto.builder()
                        .number(1)
                        .path("https://www.naver.com/")
                        .build(),
                ContentsCardDto.builder()
                        .number(2)
                        .path("https://www.google.com/")
                        .build(),
                ContentsCardDto.builder()
                        .number(3)
                        .path("https://www.daum.net/")
                        .build()
        );

        given(contentsService.getContentsCard(any(Long.class)))
                .willReturn(ContentsDto.CardListResponse.builder()
                        .title("백살도 거뜬한 건강한 치아관리 방법")
                        .cardList(Arrays.asList(
                                ContentsDto.Card.builder().number(1).path("https://www.naver.com/").build(),
                                ContentsDto.Card.builder().number(2).path("https://www.google.com/").build(),
                                ContentsDto.Card.builder().number(3).path("https://www.daum.net/").build()
                        ))
                        .build());
        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/contents/card?contentsId={contentsId}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(user("user").roles("USER"))
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("rt").value(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(document("contents/card",
                        getDocumentResponse(),
                        queryParameters(
                                parameterWithName("contentsId").description("콘텐츠 고유 번호")
                        ),
                        responseFields(
                                fieldWithPath("rt").type(JsonFieldType.NUMBER).description("결과 코드"),
                                fieldWithPath("rtMsg").type(JsonFieldType.STRING).description("결과 메세지"),
                                fieldWithPath("response").type(JsonFieldType.OBJECT).description("결과 데이터"),
                                fieldWithPath("response.title").type(JsonFieldType.STRING).description("콘텐츠 카드뉴스 제목"),
                                fieldWithPath("response.cardList").type(JsonFieldType.ARRAY).description("콘텐츠 카드뉴스 목록"),
                                fieldWithPath("response.cardList[].number").type(JsonFieldType.NUMBER).description("콘텐츠 카드뉴스 카드 번호"),
                                fieldWithPath("response.cardList[].path").type(JsonFieldType.STRING).description("콘텐츠 카드뉴스 경로")
                        )
                ));

        verify(contentsService).getContentsCard(any(Long.class));
    }

}
