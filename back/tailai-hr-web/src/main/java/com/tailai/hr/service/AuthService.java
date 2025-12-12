package com.tailai.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.entity.User;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.common.vo.UserLoginVO;
import com.tailai.hr.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 人事认证服务
 *
 * @author Tailai
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 人事登录（使用手机号登录）
     */
    public Result<UserLoginVO> login(String phone, String password) {
        // 查询用户（使用手机号登录）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .eq(User::getRole, "HR"));

        if (user == null) {
            throw new BusinessException("手机号或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 生成Token
        String token = JwtUtil.generateToken(user.getId().toString(), "HR");

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

