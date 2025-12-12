package com.me.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_messages")  //自动创建集合
public class ChatMessages {
    //唯一标识，映射到 MongoDB 文档的 _id 字段
    @Id
    private ObjectId messageId; //mongodb的id默认是objectId类型，为了自动配置id

    private Long memoryId; // 修改为Long类型，以支持时间戳作为ID

    private String content; //存储当前聊天记录列表的json字符串
}
