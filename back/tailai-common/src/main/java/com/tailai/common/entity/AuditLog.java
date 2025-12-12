package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统审计日志实体
 *
 * @author Tailai
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 用户类型：EMPLOYEE/MANAGER/HR
     */
    private String userType;

    /**
     * 操作类型：LOGIN/CREATE_CONTRACT/SIGN/ISSUE/APPROVE等
     */
    private String operation;

    /**
     * 资源类型：CONTRACT/TEMPLATE/USER等
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 操作详情（JSON格式）
     */
    private String operationDetail;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

