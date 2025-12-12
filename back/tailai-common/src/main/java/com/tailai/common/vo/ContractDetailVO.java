package com.tailai.common.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合同详情VO
 *
 * @author Tailai
 */
@Data
public class ContractDetailVO {

    /**
     * 合同ID
     */
    private Long id;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同类型
     */
    private String contractType;

    /**
     * 合同类型描述
     */
    private String contractTypeDesc;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 发起人ID
     */
    private Long initiatorId;

    /**
     * 发起人姓名
     */
    private String initiatorName;

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
     * 合同PDF URL
     */
    private String contractPdfUrl;

    /**
     * 状态码
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

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
     * 经理签名Base64（用于前端显示）
     */
    private String managerSignature;

    /**
     * 员工签名Base64（用于前端显示）
     */
    private String employeeSignature;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

