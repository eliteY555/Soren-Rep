package com.tailai.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.entity.User;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.hr.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 经理管理控制器
 *
 * @author Tailai
 */
@Tag(name = "经理管理", description = "人事管理经理账号接口")
@RestController
@RequestMapping("/api/hr/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    @Operation(summary = "注册经理账号", description = "根据真实姓名、手机号、身份证号等关键信息注册新的经理账号，使用手机号作为登录账号，默认密码123456。需要Authorization请求头")
    @PostMapping("/create")
    public Result<Long> createManager(
            @Parameter(description = "真实姓名", required = true) @RequestParam String realName,
            @Parameter(description = "手机号", required = true) @RequestParam String phone,
            @Parameter(description = "身份证号", required = true) @RequestParam String idCard,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        
        String userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return managerService.createManager(realName, phone, idCard, departmentId, Long.parseLong(userId));
    }
    
    @Operation(summary = "冻结经理账号", description = "冻结指定经理的账号。需要Authorization请求头")
    @PostMapping("/freeze/{id}")
    public Result<Void> freezeManager(
            @PathVariable Long id,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return managerService.toggleStatus(id, 0);
    }
    
    @Operation(summary = "解冻经理账号", description = "解冻指定经理的账号。需要Authorization请求头")
    @PostMapping("/unfreeze/{id}")
    public Result<Void> unfreezeManager(
            @PathVariable Long id,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return managerService.toggleStatus(id, 1);
    }

    @Operation(summary = "经理列表", description = "分页查询经理列表。需要Authorization请求头")
    @GetMapping("/list")
    public Result<Page<User>> getManagerList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return managerService.getManagerList(pageNum, pageSize);
    }

    @Operation(summary = "更新经理信息", description = "更新经理的基本信息和权限。需要Authorization请求头")
    @PutMapping("/update/{id}")
    public Result<Void> updateManager(
            @PathVariable Long id,
            @RequestParam String realName,
            @RequestParam String phone,
            @RequestParam(required = false) Long departmentId,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return managerService.updateManager(id, realName, phone, departmentId);
    }

    @Operation(summary = "重置经理密码", description = "重置为默认密码123456。需要Authorization请求头")
    @PostMapping("/reset-password/{id}")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return managerService.resetPassword(id);
    }
}

