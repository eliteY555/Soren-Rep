package com.voiceassistant.service;

import com.voiceassistant.common.dto.ChatMessage;
import com.voiceassistant.common.dto.ChatRequest;
import com.voiceassistant.common.dto.SessionDto;
import com.voiceassistant.common.enums.ChatMode;
import com.voiceassistant.common.enums.MessageRole;
import com.voiceassistant.llm.factory.ModelFactory;
import com.voiceassistant.repo.mongo.entity.ChatSession;
import com.voiceassistant.repo.mongo.entity.ChatSession.MessageEntry;
import com.voiceassistant.repo.mongo.repository.ChatSessionRepository;
import com.voiceassistant.repo.mysql.entity.ProviderConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final ChatSessionRepository sessionRepo;
    private final ConfigService configService;
    private final ModelFactory modelFactory;
    private final KnowledgeService knowledgeService;

    public ChatService(ChatSessionRepository sessionRepo,
                       ConfigService configService,
                       ModelFactory modelFactory,
                       KnowledgeService knowledgeService) {
        this.sessionRepo = sessionRepo;
        this.configService = configService;
        this.modelFactory = modelFactory;
        this.knowledgeService = knowledgeService;
    }

    /**
     * Stream LLM response. Calls onToken for each generated token,
     * onComplete when finished, onError on failure.
     * Saves both user message and assistant reply to MongoDB.
     */
    public void streamResponse(ChatRequest request,
                               Consumer<String> onToken,
                               Runnable onComplete,
                               Consumer<Throwable> onError) {
        try {
            ChatSession session = getOrCreateSession(request.getSessionId());
            ProviderConfig provider = request.getProviderId() != null
                    ? configService.findProviderEntity(request.getProviderId())
                    : configService.findProviderEntity(null);

            String decryptedApiKey = configService.decryptApiKey(provider.getApiKey());

            // Build prompt — augment with RAG context if RAG mode
            String promptContent = request.getContent();
            if (request.getMode() == ChatMode.RAG) {
                promptContent = knowledgeService.buildRagPrompt(request.getContent());
            }

            // Save user message (original content, not the augmented prompt)
            saveMessage(session, "USER", request.getContent(),
                    provider.getName(), request.getMode().name());

            // Build streaming model
            StreamingChatLanguageModel model = modelFactory
                    .getOrCreateStreamingModel(provider, decryptedApiKey);

            // Build conversation history (last 20 messages for context)
            List<dev.langchain4j.data.message.ChatMessage> history = buildHistory(session, promptContent);

            StringBuilder fullResponse = new StringBuilder();
            String sessionId = session.getId();
            String providerName = provider.getName();

            model.generate(history, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    fullResponse.append(token);
                    onToken.accept(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    // Save assistant message to the SAME session
                    ChatSession sess = sessionRepo.findById(sessionId)
                            .orElse(session);
                    saveMessage(sess, "ASSISTANT", fullResponse.toString(),
                            providerName, request.getMode().name());
                    onComplete.run();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("LLM streaming error", error);
                    // Save error message
                    ChatSession sess = sessionRepo.findById(sessionId)
                            .orElse(session);
                    saveMessage(sess, "ASSISTANT",
                            "抱歉，回答生成失败: " + error.getMessage(),
                            providerName, request.getMode().name());
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.error("ChatService error", e);
            onError.accept(e);
        }
    }

    public List<SessionDto> listSessions() {
        return sessionRepo.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    public SessionDto createSession() {
        ChatSession session = new ChatSession();
        session.setMessages(new ArrayList<>());
        ChatSession saved = sessionRepo.save(session);
        return toSessionDto(saved);
    }

    public void deleteSession(String id) {
        sessionRepo.deleteById(id);
    }

    public List<ChatMessage> getSessionMessages(String sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        return session.getMessages().stream()
                .map(m -> ChatMessage.builder()
                        .role(MessageRole.valueOf(m.getRole()))
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .providerName(m.getProviderName())
                        .mode(ChatMode.valueOf(m.getMode()))
                        .build())
                .collect(Collectors.toList());
    }

    // --- private helpers ---

    /**
     * Build conversation history from session messages plus the current prompt.
     * Keeps last 20 messages to stay within context window limits.
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildHistory(
            ChatSession session, String currentPrompt) {
        List<dev.langchain4j.data.message.ChatMessage> history = new ArrayList<>();

        // Take last 20 messages from session (excluding the just-saved user message = last one)
        List<MessageEntry> pastMessages = session.getMessages();
        int start = Math.max(0, pastMessages.size() - 21); // -21 to leave room for current
        for (int i = start; i < pastMessages.size() - 1; i++) { // exclude last (just saved)
            MessageEntry m = pastMessages.get(i);
            if ("USER".equals(m.getRole())) {
                history.add(UserMessage.from(m.getContent()));
            } else if ("ASSISTANT".equals(m.getRole())) {
                history.add(AiMessage.from(m.getContent()));
            }
        }

        // Add current prompt
        history.add(UserMessage.from(currentPrompt));

        return history;
    }

    private ChatSession getOrCreateSession(String sessionId) {
        if (sessionId != null) {
            return sessionRepo.findById(sessionId)
                    .orElseGet(this::createNewSession);
        }
        return createNewSession();
    }

    private ChatSession createNewSession() {
        ChatSession session = new ChatSession();
        session.setMessages(new ArrayList<>());
        return sessionRepo.save(session);
    }

    private void saveMessage(ChatSession session, String role, String content,
                             String providerName, String mode) {
        MessageEntry entry = new MessageEntry();
        entry.setRole(role);
        entry.setContent(content);
        entry.setTimestamp(LocalDateTime.now());
        entry.setProviderName(providerName);
        entry.setMode(mode);
        session.getMessages().add(entry);
        session.setUpdatedAt(LocalDateTime.now());
        // Auto-name session from first user message
        if (session.getMessages().size() == 1 && "USER".equals(role)) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            session.setTitle(title);
        }
        sessionRepo.save(session);
    }

    private SessionDto toSessionDto(ChatSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessages().size())
                .build();
    }
}
