package com.me.service;

import com.me.pojo.Comment;

import java.util.List;

public interface CommentService {
    int add(Comment comment);

    List<Comment> queryByRecordId(Integer recordId);

    void delete(Integer commentId);

    List<Comment> queryAll();
}
