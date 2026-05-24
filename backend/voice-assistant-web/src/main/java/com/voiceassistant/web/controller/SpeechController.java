package com.voiceassistant.web.controller;

import com.voiceassistant.common.dto.ApiResponse;
import com.voiceassistant.service.SpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping("/transcribe")
    public ApiResponse<Map<String, String>> transcribe(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "format", defaultValue = "webm") String format,
            @RequestParam(value = "language", defaultValue = "zh") String language) {

        try {
            byte[] bytes = audio.getBytes();
            log.debug("Received audio chunk: {} bytes, format={}, lang={}", bytes.length, format, language);

            String text = speechService.transcribe(bytes, format);
            return ApiResponse.success(Map.of("text", text));
        } catch (IOException e) {
            log.error("Failed to read audio upload", e);
            return ApiResponse.error(500, "Failed to process audio: " + e.getMessage());
        }
    }
}
