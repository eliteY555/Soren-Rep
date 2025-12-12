package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 *
 * @author Tailai
 */
@Data
@TableName("record")
public class Record {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 管理员ID
     */
    private Long adminId;

    /**
     * 管理员名称
     */
    private String adminName;

    /**
     * 操作内容
     */
    private String operation;

    /**
     * 操作类型：LOGIN-登录, APPROVE-审批, REJECT-驳回, CREATE-创建, UPDATE-更新, DELETE-删除
     */
    private String operationType;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}

