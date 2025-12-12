package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合同模板实体
 *
 * @author Tailai
 */
@Data
@TableName("contract_template")
public class ContractTemplate {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板类型：ONBOARD-入职，OFFBOARD-离职
     */
    private String templateType;

    /**
     * 合同分类：FORMAL-正式工，TEMP-临时工，INTERN-实习生
     */
    private String contractCategory;

    /**
     * HTML格式的模板内容
     */
    private String templateContent;

    /**
     * OSS存储路径
     */
    private String ossFilePath;

    /**
     * JSON格式的变量配置
     */
    private String variablesConfig;

    /**
     * JSON格式的签名位置配置
     */
    private String signaturePositions;

    /**
     * 版本号
     */
    private String version;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
