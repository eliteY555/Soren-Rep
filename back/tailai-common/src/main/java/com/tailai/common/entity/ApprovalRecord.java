package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录实体
 *
 * @author Tailai
 */
@Data
@TableName("approval_record")
public class ApprovalRecord implements Serializable {

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
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批结果：APPROVED-通过，REJECTED-驳回
     */
    private String approvalResult;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

