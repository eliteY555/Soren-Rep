package com.me.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Integer commentId;
    private Integer userId;
    private Integer recordId;
    private Integer role;
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    private Date createTime;
    
    private String username;
    private List<Reply> replies;
}
