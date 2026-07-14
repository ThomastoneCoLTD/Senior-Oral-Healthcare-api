package com.kaii.dentix.domain.tts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TtsDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SpeechRequest {

        @NotBlank(message = "읽을 텍스트를 입력해주세요.")
        @Size(max = 1500, message = "읽을 텍스트는 1500자 이하로 입력해주세요.")
        private String text;

        private String lang;

        private String voiceId;
    }
}
