package com.tailai.manager.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工注册返回对象
 *
 * @author Tailai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "员工注册返回对象")
public class EmployeeRegisterVO {

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号（用户名）")
    private String phone;

    @Schema(description = "默认密码")
    private String defaultPassword;
}

