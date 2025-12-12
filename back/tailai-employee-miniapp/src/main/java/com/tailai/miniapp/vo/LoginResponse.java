package com.tailai.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工登录响应
 *
 * @author Tailai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "员工登录响应")
public class LoginResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "Token过期时间（毫秒）")
    private Long expiresIn;
}







