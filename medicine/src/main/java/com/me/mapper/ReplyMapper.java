package com.me.mapper;

import com.me.pojo.Reply;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReplyMapper {
    int add(Reply reply);

    List<Reply> queryAll(Integer commentId);

    void delete(Integer replyId);
}
