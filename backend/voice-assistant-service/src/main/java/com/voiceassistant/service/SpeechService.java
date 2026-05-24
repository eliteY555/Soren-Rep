package com.voiceassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Speech-to-text via Qwen DashScope Paraformer.
 * Accepts audio chunks from the frontend, forwards to Qwen for transcription.
 */
@Slf4j
@Service
public class SpeechService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public SpeechService(
            @Value("${speech.api-key:${QWEN_KEY:}}") String apiKey,
            @Value("${speech.base-url:https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription}") String baseUrl,
            @Value("${speech.model:paraformer-v2}") String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Transcribe an audio chunk.
     *
     * @param audioBytes raw audio data (webm/opus from browser MediaRecorder)
     * @param format     audio format hint passed from frontend
     * @return transcribed text, or empty string on failure
     */
    public String transcribe(byte[] audioBytes, String format) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("QWEN_KEY not configured — speech recognition unavailable");
            return "";
        }
        if (audioBytes == null || audioBytes.length < 100) return "";

        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            String fmt = format != null ? format : "wav";
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", Map.of("audio", base64Audio),
                    "parameters", Map.of(
                            "format", fmt,
                            "sample_rate", 16000,
                            "language_hints", java.util.List.of("zh", "en")
                    )
            );

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Qwen STT response: status={}, body={}", response.statusCode(),
                    response.body().length() > 200 ? response.body().substring(0, 200) : response.body());

            if (response.statusCode() == 200) {
                return extractText(response.body());
            } else {
                log.warn("Qwen STT returned status {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Speech transcription failed: {}", e.getMessage());
        }
        return "";
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode output = root.path("output");
            if (output.isMissingNode()) {
                // Try alternative response format
                JsonNode text = root.at("/output/sentences/0/text");
                if (!text.isMissingNode()) return text.asText();
                return "";
            }
            // Standard format: output.text or output.sentences[0].text
            JsonNode text = output.path("text");
            if (!text.isMissingNode()) return text.asText();

            JsonNode sentences = output.path("sentences");
            if (sentences.isArray() && sentences.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode s : sentences) {
                    JsonNode t = s.path("text");
                    if (!t.isMissingNode()) sb.append(t.asText());
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse STT response: {}", e.getMessage());
        }
        return "";
    }
}
