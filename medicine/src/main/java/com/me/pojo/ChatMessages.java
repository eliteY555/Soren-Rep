package com.me.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_messages")
public class ChatMessages {
    @Id
    private ObjectId messageId;

    @Indexed(unique = true)
    private Long memoryId;

    private String content;

    /**
     * 最后更新时间，MongoDB TTL 索引自动删除 7 天前的文档
     */
    @Indexed(expireAfterSeconds = 604800) // 604800 秒 = 7 天
    private Date updatedAt;
}
