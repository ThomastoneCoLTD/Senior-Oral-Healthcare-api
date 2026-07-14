package com.kaii.dentix.domain.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaii.dentix.domain.tts.application.TtsService;
import com.kaii.dentix.domain.tts.controller.TtsController;
import com.kaii.dentix.domain.tts.dto.TtsDto;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TtsController.class)
@ExtendWith(RestDocumentationExtension.class)
public class TtsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TtsService ttsService;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void speechReturnsMp3Bytes() throws Exception {
        TtsDto.SpeechRequest request = new TtsDto.SpeechRequest();
        request.setText("안녕하세요.");
        request.setLang("ko");

        byte[] audio = new byte[]{1, 2, 3};
        given(ttsService.synthesize(any(TtsDto.SpeechRequest.class))).willReturn(audio);

        mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/tts/speech")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept("audio/mpeg")
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "tts.고유경.AccessToken")
                                .with(user("user").roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(audio))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tts.mp3\""));

        verify(ttsService).synthesize(any(TtsDto.SpeechRequest.class));
    }
}
