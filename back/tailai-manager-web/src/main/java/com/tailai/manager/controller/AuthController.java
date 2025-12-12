package com.tailai.manager.controller;

import com.tailai.common.dto.ManagerLoginRequest;
import com.tailai.common.result.Result;
import com.tailai.common.vo.UserLoginVO;
import com.tailai.manager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author Tailai
 */
@Tag(name = "认证管理", description = "经理登录认证接口")
@RestController
@RequestMapping("/api/manager/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "经理登录", description = "使用用户名和密码登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Validated @RequestBody ManagerLoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        // TODO: 将token加入黑名单
        return Result.success();
    }
}

