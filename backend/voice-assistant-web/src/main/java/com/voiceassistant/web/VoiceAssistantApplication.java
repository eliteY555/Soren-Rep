package com.voiceassistant.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.voiceassistant")
@EnableJpaRepositories(basePackages = "com.voiceassistant.repo.mysql.repository")
@EntityScan(basePackages = "com.voiceassistant.repo.mysql.entity")
@EnableMongoRepositories(basePackages = "com.voiceassistant.repo.mongo.repository")
public class VoiceAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceAssistantApplication.class, args);
    }
}
