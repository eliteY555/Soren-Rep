package com.voiceassistant.repo.mongo.repository;

import com.voiceassistant.repo.mongo.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findAllByOrderByUpdatedAtDesc();
}
