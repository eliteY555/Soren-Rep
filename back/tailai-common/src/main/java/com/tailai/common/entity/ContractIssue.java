package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合同下发记录实体
 *
 * @author Tailai
 */
@Data
@TableName("contract_issue")
public class ContractIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 下发经理ID
     */
    private Long managerId;

    /**
     * 目标员工ID
     */
    private Long employeeId;

    /**
     * 下发时间
     */
    private LocalDateTime issueTime;

    /**
     * 员工签署时间
     */
    private LocalDateTime employeeSignTime;

    /**
     * 状态：1-已下发待签，2-员工已签
     */
    private Integer status;

    /**
     * 是否已发送通知：0-未发送，1-已发送
     */
    private Integer notificationSent;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

