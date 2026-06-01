package com.me.controller;

import com.me.assistant.AgentAssistant;
import com.me.pojo.ChatForm;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(value = "/agent")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentAssistant agentAssistant;

    /**
     * AI 流式对话
     *
     * Servlet 线程调用 blockLast() 阻塞直到 LLM 输出完毕。
     * 每个 token 到达时（Netty event loop 线程）直接 write + flush 到底层 socket。
     * 无 SSE 包装、无中间 buffer、无线程切换。
     */
    @PostMapping(value = "/chat")
    public void chat(@RequestBody ChatForm chatForm, HttpServletResponse response) {
        if (chatForm.getMemoryId() == null) {
            chatForm.setMemoryId(System.currentTimeMillis());
        }

        long id = chatForm.getMemoryId();
        String msg = chatForm.getMessage();

        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setBufferSize(0);

        try {
            OutputStream out = response.getOutputStream();
            logger.info("stream start memoryId={}", id);

            agentAssistant.chat(id, msg)
                    .doOnNext(token -> {
                        try {
                            out.write(token.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        } catch (IOException e) {
                            throw new RuntimeException("client disconnected", e);
                        }
                    })
                    .doFinally(s -> logger.info("stream end memoryId={}", id))
                    .blockLast();

        } catch (Exception e) {
            logger.error("stream error memoryId={}: {}", id, e.getMessage());
        }
    }
}
