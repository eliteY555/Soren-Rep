package com.tailai.hr.controller;

import com.tailai.common.result.Result;
import com.tailai.common.vo.UserLoginVO;
import com.tailai.hr.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 人事认证控制器
 *
 * @author Tailai
 */
@Tag(name = "认证管理", description = "人事登录认证接口")
@RestController
@RequestMapping("/api/hr/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "人事登录", description = "使用手机号和密码登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(
            @RequestParam String phone,
            @RequestParam String password) {
        return authService.login(phone, password);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        // TODO: 将token加入黑名单
        return Result.success();
    }
}

