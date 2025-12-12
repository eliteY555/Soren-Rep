package com.me.service;

import com.me.pojo.Reply;

import java.util.List;

public interface ReplyService {
    int add(Reply reply);

    List<Reply> queryAll(Integer commentId);

    void delete(Integer replyId);
}
