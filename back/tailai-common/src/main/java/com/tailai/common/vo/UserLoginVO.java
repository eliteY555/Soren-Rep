package com.tailai.common.vo;

import lombok.Data;

/**
 * 用户登录响应VO
 *
 * @author Tailai
 */
@Data
public class UserLoginVO {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 手机号（登录账号）
     */
    private String phone;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 用户角色：EMPLOYEE/MANAGER/HR
     */
    private String role;
}

