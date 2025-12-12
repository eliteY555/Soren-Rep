package com.me.mapper;

import com.me.pojo.Comment;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CommentMapper {
    int add(Comment comment);

    List<Comment> queryByRecordId(Integer recordId);

    void delete(Integer commentId);

    List<Comment> queryBy();
}
