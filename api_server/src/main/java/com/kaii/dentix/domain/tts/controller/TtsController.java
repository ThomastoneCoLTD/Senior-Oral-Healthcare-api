package com.kaii.dentix.domain.tts.controller;

import com.kaii.dentix.domain.tts.application.TtsService;
import com.kaii.dentix.domain.tts.dto.TtsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tts")
public class TtsController {

    private final TtsService ttsService;

    @PostMapping(value = "/speech", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesizeSpeech(@Valid @RequestBody TtsDto.SpeechRequest request) {
        byte[] audio = ttsService.synthesize(request);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tts.mp3\"")
                .body(audio);
    }
}
