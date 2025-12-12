package com.tailai.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 更新用户信息请求
 *
 * @author Tailai
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserInfoRequest {

    @Schema(description = "真实姓名")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "身份证号")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$", 
             message = "身份证号格式不正确")
    private String idCard;

    @Schema(description = "性别：1-男，2-女")
    private Integer gender;
}

