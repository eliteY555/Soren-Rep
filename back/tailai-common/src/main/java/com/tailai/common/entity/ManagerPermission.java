package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 经理权限配置实体
 *
 * @author Tailai
 */
@Data
@TableName("manager_permission")
public class ManagerPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 经理ID
     */
    private Long managerId;

    /**
     * 可管理的部门ID（NULL表示不限制）
     */
    private Long departmentId;

    /**
     * 是否可发起合同：0-否，1-是
     */
    private Integer canIssueContract;

    /**
     * 是否可管理员工：0-否，1-是
     */
    private Integer canManageEmployee;

    /**
     * 创建人（人事ID）
     */
    private Long createdBy;

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

