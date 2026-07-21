package com.kaii.dentix.domain.tts.application;

import com.kaii.dentix.domain.tts.dto.TtsDto;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

@Service
@RequiredArgsConstructor
public class TtsService {

    private static final int MAX_TEXT_LENGTH = 1500;

    private final PollyClient pollyClient;

    public byte[] synthesize(TtsDto.SpeechRequest request) {
        String text = normalizeText(request.getText());
        VoiceId voiceId = resolveVoiceId(request.getLang(), request.getVoiceId());

        SynthesizeSpeechRequest speechRequest = SynthesizeSpeechRequest.builder()
                .text(text)
                .textType(TextType.TEXT)
                .voiceId(voiceId)
                .outputFormat(OutputFormat.MP3)
                .build();

        ResponseBytes<SynthesizeSpeechResponse> response =
                pollyClient.synthesizeSpeech(speechRequest, ResponseTransformer.toBytes());

        return response.asByteArray();
    }

    private String normalizeText(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestApiException("읽을 텍스트를 입력해주세요.");
        }
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestApiException("읽을 텍스트는 1500자 이하로 입력해주세요.");
        }
        return normalized;
    }

    private VoiceId resolveVoiceId(String lang, String voiceId) {
        if (StringUtils.hasText(voiceId)) {
            return VoiceId.fromValue(voiceId.trim());
        }

        String normalizedLang = lang == null ? "ko" : lang.trim().toLowerCase();
        if (normalizedLang.startsWith("en")) {
            return VoiceId.fromValue("Joanna");
        }
        if (normalizedLang.startsWith("vi")) {
            return VoiceId.fromValue("Linh");
        }
        if (normalizedLang.startsWith("ja")) {
            return VoiceId.fromValue("Mizuki");
        }
        if (normalizedLang.startsWith("zh")) {
            return VoiceId.fromValue("Zhiyu");
        }
        return VoiceId.fromValue("Seoyeon");
    }
}
