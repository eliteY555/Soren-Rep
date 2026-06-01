package com.me.store;

import com.me.pojo.ChatMessages;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 会话记忆 — MongoDB 持久化
 *
 * 内存窗口：最近 20 条消息（CommonConfig.MessageWindowChatMemory.maxMessages=20）
 * 持久窗口：MongoDB TTL 索引 7 天自动清理
 * memoryId：与 userId 一一对应，每个用户独立会话
 */

@Component
public class MemoryStore implements ChatMemoryStore {

    private static final Logger logger = LoggerFactory.getLogger(MemoryStore.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);

        ChatMessages messages = mongoTemplate.findOne(query, ChatMessages.class);

        if (messages == null) {
            logger.debug("未找到会话记忆 memoryId={}, 返回空列表", memoryId);
            return new LinkedList<>();
        }

        try {
            List<ChatMessage> chatMessages = ChatMessageDeserializer.messagesFromJson(messages.getContent());
            logger.debug("加载会话记忆 memoryId={}, 消息数={}", memoryId, chatMessages.size());
            return chatMessages;
        } catch (Exception e) {
            logger.error("反序列化会话记忆失败 memoryId={}: {}", memoryId, e.getMessage());
            return new LinkedList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", ChatMessageSerializer.messagesToJson(list));
        update.set("updatedAt", new Date()); // 刷新 TTL 计时
        // 不存在则插入，存在则更新
        mongoTemplate.upsert(query, update, ChatMessages.class);
        logger.debug("持久化会话记忆 memoryId={}, 消息数={}", memoryId, list.size());
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
        logger.debug("删除会话记忆 memoryId={}", memoryId);
    }
}
