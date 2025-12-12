package com.tailai.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 管理员登录请求
 *
 * @author Tailai
 */
@Data
@Schema(description = "管理员登录请求")
public class AdminLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;
}

