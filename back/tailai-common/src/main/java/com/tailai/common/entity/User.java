package com.tailai.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体（新流程：支持员工、经理、人事）
 *
 * @author Tailai
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 身份证号（加密存储）
     */
    private String idCard;

    /**
     * 性别：0-女，1-男
     */
    private Integer gender;

    /**
     * 用户角色：EMPLOYEE-员工，MANAGER-经理，HR-人事
     */
    private String role;

    /**
     * 所属经理ID（员工专用）
     */
    private Long managedBy;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

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

