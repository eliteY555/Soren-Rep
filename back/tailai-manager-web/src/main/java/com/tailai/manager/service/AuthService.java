package com.tailai.manager.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.dto.ManagerLoginRequest;
import com.tailai.common.entity.User;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.common.vo.UserLoginVO;
import com.tailai.manager.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 *
 * @author Tailai
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 经理登录
     */
    public Result<UserLoginVO> login(ManagerLoginRequest request) {
        // 查询用户（使用手机号登录）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())
                .eq(User::getRole, "MANAGER"));

        if (user == null) {
            throw new BusinessException("手机号或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId().toString(), "MANAGER");

        // 构造返回VO
        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());

        return Result.success(vo);
    }
}

