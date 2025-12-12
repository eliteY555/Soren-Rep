package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子合同实体（新流程）
 *
 * @author Tailai
 */
@Data
@TableName("contract")
public class Contract implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同类型：ONBOARD-入职，OFFBOARD-离职
     */
    private String contractType;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 发起人ID（经理ID）
     */
    private Long initiatorId;

    /**
     * 发起人姓名
     */
    private String initiatorName;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 岗位
     */
    private String position;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 合同开始日期
     */
    private LocalDate startDate;

    /**
     * 合同结束日期
     */
    private LocalDate endDate;

    /**
     * 合同PDF的OSS路径
     */
    private String contractPdfUrl;

    /**
     * 合同状态：1-待员工签字，2-已生效，3-已终止
     */
    private Integer status;

    /**
     * 发起人签署时间
     */
    private LocalDateTime initiatorSignTime;

    /**
     * 下发时间
     */
    private LocalDateTime issueTime;

    /**
     * 员工签署时间
     */
    private LocalDateTime employeeSignTime;

    /**
     * 合同生效时间
     */
    private LocalDateTime effectiveTime;

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

