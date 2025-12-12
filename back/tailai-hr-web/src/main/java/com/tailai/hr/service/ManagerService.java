package com.tailai.hr.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.entity.ManagerPermission;
import com.tailai.common.entity.User;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.hr.mapper.ManagerPermissionMapper;
import com.tailai.hr.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 经理管理服务
 *
 * @author Tailai
 */
@Service
@RequiredArgsConstructor
public class ManagerService {

    private final UserMapper userMapper;
    private final ManagerPermissionMapper managerPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 创建经理账号（使用手机号作为登录账号，根据真实姓名、手机号、身份证号等关键信息注册）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createManager(String realName, String phone, String idCard, Long departmentId, Long createdBy) {
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

        // 创建用户（使用手机号登录，默认密码123456）
        User user = new User();
        user.setPassword(passwordEncoder.encode("123456")); // 默认密码
        user.setRealName(realName);
        user.setPhone(phone);
        user.setIdCard(idCard); // 保存身份证号（实际应用中应加密存储）
        user.setRole("MANAGER");
        user.setDepartmentId(departmentId);
        user.setStatus(1);
        userMapper.insert(user);

        // 创建权限记录
        ManagerPermission permission = new ManagerPermission();
        permission.setManagerId(user.getId());
        permission.setDepartmentId(departmentId);
        permission.setCanIssueContract(1);
        permission.setCanManageEmployee(1);
        permission.setCreatedBy(createdBy);
        managerPermissionMapper.insert(permission);

        return Result.success(user.getId());
    }

    /**
     * 查询经理列表
     */
    public Result<Page<User>> getManagerList(int pageNum, int pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "MANAGER")
                .orderByDesc(User::getCreatedAt);
        
        Page<User> result = userMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 更新经理信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateManager(Long id, String realName, String phone, Long departmentId) {
        User user = userMapper.selectById(id);
        if (user == null || !"MANAGER".equals(user.getRole())) {
            throw new BusinessException("经理不存在");
        }

        user.setRealName(realName);
        user.setPhone(phone);
        user.setDepartmentId(departmentId);
        userMapper.updateById(user);

        // 更新权限
        ManagerPermission permission = managerPermissionMapper.selectOne(
                new LambdaQueryWrapper<ManagerPermission>()
                        .eq(ManagerPermission::getManagerId, id));
        if (permission != null) {
            permission.setDepartmentId(departmentId);
            managerPermissionMapper.updateById(permission);
        }

        return Result.success();
    }

    /**
     * 重置经理密码
     */
    public Result<Void> resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || !"MANAGER".equals(user.getRole())) {
            throw new BusinessException("经理不存在");
        }

        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);

        return Result.success();
    }

    /**
     * 启用/禁用经理账号
     */
    public Result<Void> toggleStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null || !"MANAGER".equals(user.getRole())) {
            throw new BusinessException("经理不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        return Result.success();
    }
}

