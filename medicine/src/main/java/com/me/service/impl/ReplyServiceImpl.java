package com.me.service.impl;

import com.me.mapper.ReplyMapper;
import com.me.pojo.Reply;
import com.me.service.ReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReplyServiceImpl implements ReplyService {
    @Autowired
    private ReplyMapper replyMapper;
    @Override
    public int add(Reply reply) {
        return replyMapper.add(reply);
    }

    @Override
    public List<Reply> queryAll(Integer commentId) {
        return replyMapper.queryAll(commentId);
    }

    @Override
    public void delete(Integer replyId) {
        replyMapper.delete(replyId);
    }
}
