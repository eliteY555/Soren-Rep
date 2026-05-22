package com.voiceassistant.web.controller;

import com.voiceassistant.common.dto.ApiResponse;
import com.voiceassistant.common.dto.ChatMessage;
import com.voiceassistant.common.dto.ChatRequest;
import com.voiceassistant.common.dto.SessionDto;
import com.voiceassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout

        executor.execute(() -> {
            chatService.streamResponse(request,
                    token -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    emitter::completeWithError
            );
        });

        return emitter;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionDto>> listSessions() {
        return ApiResponse.success(chatService.listSessions());
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionDto> createSession() {
        return ApiResponse.success(chatService.createSession());
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable String id) {
        chatService.deleteSession(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable String id) {
        return ApiResponse.success(chatService.getSessionMessages(id));
    }
}
