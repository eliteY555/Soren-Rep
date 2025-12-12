package com.tailai.miniapp.controller;

import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.miniapp.dto.ChangePasswordRequest;
import com.tailai.miniapp.dto.UpdateUserInfoRequest;
import com.tailai.miniapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息控制器
 *
 * @author Tailai
 */
@Slf4j
@Tag(name = "用户管理", description = "用户信息查询、更新")
@RestController
@RequestMapping("/api/miniapp/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的基本信息")
    @PostMapping("/update")
    public Result<Void> updateUserInfo(
            @Parameter(description = "更新用户信息请求") @Validated @RequestBody UpdateUserInfoRequest request,
            @Parameter(description = "JWT Token", required = true) @RequestHeader("Authorization") String authorization) {
        
        // 从Token中获取用户ID
        Long userId = getUserIdFromToken(authorization);
        
        log.info("用户 {} 请求更新信息", userId);
        userService.updateUserInfo(userId, request);
        
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(
            @Parameter(description = "修改密码请求") @Validated @RequestBody ChangePasswordRequest request,
            @Parameter(description = "JWT Token", required = true) @RequestHeader("Authorization") String authorization) {
        
        // 从Token中获取用户ID
        Long userId = getUserIdFromToken(authorization);
        
        log.info("用户 {} 请求修改密码", userId);
        userService.changePassword(userId, request);
        
        return Result.success();
    }

    /**
     * 从请求头中获取用户ID
     */
    private Long getUserIdFromToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("未登录或Token无效");
        }
        
        String token = authorization.substring(7);
        return JwtUtil.getUserIdAsLong(token);
    }
}




