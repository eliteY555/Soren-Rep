package com.me.controller;

import com.me.common.Result;
import com.me.pojo.Reply;
import com.me.service.ReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reply")
public class ReplyController {

    @Autowired
    private ReplyService replyService;

    @PostMapping("/add")
    public Result addReply(@RequestBody Reply reply) {
        try {
            int result = replyService.add(reply);
            if (result > 0) {
                return Result.success(reply);
            } else {
                return Result.error("添加回复失败");
            }
        } catch (Exception e) {
            return Result.error("添加回复失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{commentId}")
    public Result getReplyList(@PathVariable("commentId") Integer commentId) {
        List<Reply> replyList = replyService.queryAll(commentId);
        return Result.success(replyList);
    }

    @DeleteMapping("/delete/{replyId}")
    public Result deleteReply(@PathVariable("replyId") Integer replyId) {
        try {
            replyService.delete(replyId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("删除回复失败: " + e.getMessage());
        }
    }
}
