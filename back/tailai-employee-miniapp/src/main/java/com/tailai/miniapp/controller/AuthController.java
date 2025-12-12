package com.tailai.miniapp.controller;

import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.miniapp.dto.LoginRequest;
import com.tailai.miniapp.service.AuthService;
import com.tailai.miniapp.vo.LoginResponse;
import com.tailai.miniapp.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 员工认证控制器
 *
 * @author Tailai
 */
@Slf4j
@Tag(name = "员工认证", description = "员工登录、用户信息管理")
@RestController
@RequestMapping("/api/miniapp/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 员工登录（使用手机号和密码登录）
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录", description = "使用手机号和密码登录")
    public Result<LoginResponse> login(
            @Validated @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("========== 员工登录请求 ==========");
        log.info("手机号：{}", request.getPhone());

        LoginResponse response = authService.login(request, httpRequest);

        log.info("========== 员工登录成功 ==========");
        log.info("用户ID：{}，手机号：{}", response.getUserId(), response.getPhone());

        return Result.success("登录成功", response);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "清除登录状态")
    public Result<String> logout() {
        log.info("员工退出登录");
        // 前端清除Token即可，后端无需处理
        return Result.success("退出成功", "退出登录成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/userinfo")
    @Operation(summary = "获取用户信息", description = "根据Token获取当前登录用户的详细信息")
    @Parameter(name = "Authorization", description = "JWT Token", required = true, example = "Bearer xxx")
    public Result<UserInfoVO> getUserInfo(
            @RequestHeader("Authorization") String authorization
    ) {
        // 从Token中提取用户ID
        String token = authorization.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdAsLong(token);

        if (userId == null) {
            return Result.error(401, "Token无效");
        }

        log.info("获取用户信息，用户ID：{}", userId);

        UserInfoVO userInfo = authService.getUserInfo(userId);

        return Result.success("获取成功", userInfo);
    }

    /**
     * 验证Token是否有效
     */
    @GetMapping("/validate")
    @Operation(summary = "验证Token", description = "验证Token是否有效")
    @Parameter(name = "Authorization", description = "JWT Token", required = true, example = "Bearer xxx")
    public Result<Boolean> validateToken(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.replace("Bearer ", "");
        boolean isValid = JwtUtil.validateToken(token);

        if (isValid) {
            return Result.success("Token有效", true);
        } else {
            return Result.error(401, "Token已过期或无效");
        }
    }
}

