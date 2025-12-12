package com.tailai.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.entity.User;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.manager.mapper.UserMapper;
import com.tailai.manager.vo.EmployeeRegisterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工管理服务
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    
    private static final String DEFAULT_PASSWORD = "123456";

    /**
     * 注册员工账号（根据真实姓名、手机号、身份证号等关键信息注册）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<EmployeeRegisterVO> registerEmployee(String realName, String phone, String idCard, Integer gender, String token) {
        // 从token获取当前经理ID
        String managerId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        // 检查手机号是否已存在
        User existPhone = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (existPhone != null) {
            throw new BusinessException("手机号已被使用");
        }
        
        // 检查身份证号是否已存在
        User existIdCard = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getIdCard, idCard));
        if (existIdCard != null) {
            throw new BusinessException("身份证号已被使用");
        }

        // 创建员工账号，使用手机号作为登录凭证，设置默认密码
        User employee = new User();
        employee.setRealName(realName);
        employee.setPhone(phone);
        employee.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD)); // 加密存储默认密码
        employee.setIdCard(idCard); // 保存身份证号（实际应用中应加密存储）
        employee.setGender(gender);
        employee.setRole("EMPLOYEE");
        employee.setManagedBy(Long.parseLong(managerId)); // 设置所属经理
        employee.setStatus(1); // 默认启用
        userMapper.insert(employee);

        log.info("经理{}注册员工成功，员工ID：{}，手机号：{}", managerId, employee.getId(), phone);
        
        // 返回注册信息，包括手机号（用户名）和默认密码
        EmployeeRegisterVO vo = EmployeeRegisterVO.builder()
                .employeeId(employee.getId())
                .realName(realName)
                .phone(phone)
                .defaultPassword(DEFAULT_PASSWORD)
                .build();
        
        return Result.success(vo);
    }

    /**
     * 查询我的员工列表
     */
    public Result<Page<User>> getMyEmployees(int pageNum, int pageSize, String token) {
        // 从token获取当前经理ID
        String managerId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getManagedBy, Long.parseLong(managerId))
                .eq(User::getRole, "EMPLOYEE")
                .orderByDesc(User::getCreatedAt);
        
        Page<User> result = userMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 冻结/解冻员工账号
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleStatus(Long employeeId, Integer status, String token) {
        // 从token获取当前经理ID
        String managerId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        // 检查员工是否存在且属于当前经理
        User employee = userMapper.selectById(employeeId);
        if (employee == null || !"EMPLOYEE".equals(employee.getRole())) {
            throw new BusinessException("员工不存在");
        }
        
        if (!employee.getManagedBy().equals(Long.parseLong(managerId))) {
            throw new BusinessException("您无权操作该员工");
        }

        employee.setStatus(status);
        userMapper.updateById(employee);

        log.info("经理{}{}员工{}账号", managerId, status == 0 ? "冻结" : "解冻", employeeId);
        return Result.success();
    }

    /**
     * 获取员工详情
     */
    public Result<User> getEmployeeDetail(Long employeeId, String token) {
        // 从token获取当前经理ID
        String managerId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        // 检查员工是否存在且属于当前经理
        User employee = userMapper.selectById(employeeId);
        if (employee == null || !"EMPLOYEE".equals(employee.getRole())) {
            throw new BusinessException("员工不存在");
        }
        
        if (!employee.getManagedBy().equals(Long.parseLong(managerId))) {
            throw new BusinessException("您无权查看该员工信息");
        }

        return Result.success(employee);
    }

    /**
     * 重置员工密码为默认密码
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resetPassword(Long employeeId, String token) {
        // 从token获取当前经理ID
        String managerId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        // 检查员工是否存在且属于当前经理
        User employee = userMapper.selectById(employeeId);
        if (employee == null || !"EMPLOYEE".equals(employee.getRole())) {
            throw new BusinessException("员工不存在");
        }
        
        if (!employee.getManagedBy().equals(Long.parseLong(managerId))) {
            throw new BusinessException("您无权操作该员工");
        }

        // 重置密码为默认密码
        employee.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.updateById(employee);

        log.info("经理{}重置员工{}密码成功", managerId, employeeId);
        return Result.success();
    }
}

