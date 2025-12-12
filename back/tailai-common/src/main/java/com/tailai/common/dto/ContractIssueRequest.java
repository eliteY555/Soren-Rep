package com.tailai.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同下发请求DTO（经理发起合同并签字后下发给员工）
 *
 * @author Tailai
 */
@Data
@Schema(description = "合同下发请求")
public class ContractIssueRequest {

    /**
     * 模板ID
     */
    @Schema(description = "模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 目标员工ID
     */
    @Schema(description = "目标员工ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /**
     * 岗位
     */
    @Schema(description = "岗位")
    private String position;

    /**
     * 基本工资
     */
    @Schema(description = "基本工资")
    private BigDecimal baseSalary;

    /**
     * 合同开始日期
     */
    @Schema(description = "合同开始日期")
    private LocalDate startDate;

    /**
     * 合同结束日期
     */
    @Schema(description = "合同结束日期")
    private LocalDate endDate;

    /**
     * 经理签名Base64数据（发起时必须签字）
     */
    @Schema(description = "经理签名Base64数据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名数据不能为空")
    private String signatureBase64;

    /**
     * 签署IP地址
     */
    @Schema(description = "签署IP地址")
    private String ipAddress;

    /**
     * 设备信息
     */
    @Schema(description = "设备信息")
    private String deviceInfo;

    /**
     * 是否发送通知
     */
    @Schema(description = "是否发送通知", defaultValue = "true")
    private Boolean sendNotification = true;
}

