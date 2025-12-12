package com.tailai.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.entity.User;
import com.tailai.common.result.Result;
import com.tailai.manager.service.EmployeeService;
import com.tailai.manager.vo.EmployeeRegisterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 员工管理控制器
 *
 * @author Tailai
 */
@Tag(name = "员工管理", description = "经理管理员工账号接口")
@RestController
@RequestMapping("/api/manager/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "注册员工账号", description = "根据真实姓名、手机号、身份证号等关键信息注册新的员工账号，返回手机号（用户名）和默认密码123456")
    @PostMapping("/register")
    public Result<EmployeeRegisterVO> registerEmployee(
            @Parameter(description = "真实姓名") @RequestParam String realName,
            @Parameter(description = "手机号") @RequestParam String phone,
            @Parameter(description = "身份证号") @RequestParam String idCard,
            @Parameter(description = "性别：0-女，1-男") @RequestParam(required = false) Integer gender,
            @RequestHeader("Authorization") String token) {
        
        return employeeService.registerEmployee(realName, phone, idCard, gender, token);
    }
    
    @Operation(summary = "我的员工列表", description = "查询当前经理管理的所有员工")
    @GetMapping("/my-list")
    public Result<Page<User>> getMyEmployees(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader("Authorization") String token) {
        return employeeService.getMyEmployees(pageNum, pageSize, token);
    }

    @Operation(summary = "冻结员工账号", description = "冻结指定员工的账号")
    @PostMapping("/freeze/{id}")
    public Result<Void> freezeEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return employeeService.toggleStatus(id, 0, token);
    }
    
    @Operation(summary = "解冻员工账号", description = "解冻指定员工的账号")
    @PostMapping("/unfreeze/{id}")
    public Result<Void> unfreezeEmployee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return employeeService.toggleStatus(id, 1, token);
    }
    
    @Operation(summary = "员工详情", description = "查看员工详细信息")
    @GetMapping("/detail/{id}")
    public Result<User> getEmployeeDetail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return employeeService.getEmployeeDetail(id, token);
    }

    @Operation(summary = "重置员工密码", description = "将员工密码重置为默认密码123456")
    @PostMapping("/reset-password/{id}")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return employeeService.resetPassword(id, token);
    }
}

