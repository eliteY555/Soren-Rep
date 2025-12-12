package com.tailai.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 经理登录请求DTO
 *
 * @author Tailai
 */
@Data
public class ManagerLoginRequest {

    /**
     * 手机号（登录账号）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}

