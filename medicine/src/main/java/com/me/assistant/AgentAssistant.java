package com.me.assistant;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
//        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "MemoryProvider",
        tools = "agentTools", //LLM 根据问题语义自动判断是否调用工具
        contentRetriever = "contentRetrieverPinecone" //内容检索
)

public interface AgentAssistant {
    @SystemMessage(fromResource = "SystemPrompt.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
