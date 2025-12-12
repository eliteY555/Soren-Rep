package com.tailai.miniapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.entity.User;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.util.JwtUtil;
import com.tailai.miniapp.dto.LoginRequest;
import com.tailai.miniapp.mapper.UserMapper;
import com.tailai.miniapp.vo.LoginResponse;
import com.tailai.miniapp.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 员工认证服务
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 员工登录（使用手机号登录）
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // 1. 根据手机号查询员工用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, request.getPhone())
                .eq(User::getRole, "EMPLOYEE");
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(400, "手机号或密码错误");
        }

        // 2. 验证密码（使用BCrypt加密验证）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "手机号或密码错误");
        }

        // 3. 检查账号状态
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 更新最后登录时间和IP
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(getIpAddress(httpRequest));
        userMapper.updateById(user);

        // 5. 生成Token
        String token = JwtUtil.generateToken(user.getId(), user.getPhone());

        // 6. 构建响应
        return LoginResponse.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .token(token)
                .expiresIn(60 * 60 * 1000L) // 1小时，单位：毫秒
                .build();
    }

    /**
     * 获取用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 转换为VO
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);

        // 脱敏处理：身份证号只显示前6位和后4位
        if (user.getIdCard() != null && user.getIdCard().length() >= 10) {
            String idCard = user.getIdCard();
            vo.setIdCard(idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4));
        }

        return vo;
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

