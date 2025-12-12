package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工申请记录实体
 *
 * @author Tailai
 */
@Data
@TableName("application")
public class Application implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 申请类型：ONBOARD-入职，OFFBOARD-离职
     */
    private String applicationType;

    /**
     * 岗位
     */
    private String position;

    /**
     * 入职开始日期（入职申请必填）
     */
    private LocalDate startDate;

    /**
     * 合同结束日期（入职申请）
     */
    private LocalDate endDate;

    /**
     * 离职日期（离职申请必填）
     */
    private LocalDate offboardDate;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 使用的模板ID
     */
    private Long templateId;

    /**
     * PDF文件路径
     */
    private String pdfUrl;

    /**
     * 状态：PENDING-待审批，APPROVED-已通过，REJECTED-已驳回
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

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

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

