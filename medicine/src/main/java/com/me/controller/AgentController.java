package com.me.controller;
import com.alibaba.dashscope.assistants.Assistant;
import com.me.assistant.AgentAssistant;
import com.me.pojo.ChatForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/agent")
public class AgentController {
    @Autowired
    private AgentAssistant assistant;

    @PostMapping( value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm){ //将请求体内容转换为 ChatForm 对象
        // 如果memoryId为null，设置一个默认值
        if (chatForm.getMemoryId() == null) {
            chatForm.setMemoryId(System.currentTimeMillis()); // 使用当前时间戳作为默认memoryId
        }
        
        // 获取memoryId，确保它是Long类型
        Long memoryId = chatForm.getMemoryId();
        String message = chatForm.getMessage();
        
        // 记录接收到的请求信息
        System.out.println("接收到聊天请求 - memoryId: " + memoryId + ", message: " + message);
        
        return assistant.chat(memoryId, message);
    }
}