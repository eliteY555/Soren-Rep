package com.tailai.manager.interceptor;

import cn.hutool.core.util.StrUtil;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器
 *
 * @author Tailai
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取Token
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            throw new BusinessException("未登录或登录已过期");
        }

        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证Token
        if (!JwtUtil.validateToken(token)) {
            throw new BusinessException("Token无效或已过期");
        }

        // 验证用户类型
        String userType = JwtUtil.getUserTypeFromToken(token);
        if (!"MANAGER".equals(userType)) {
            throw new BusinessException("无权限访问");
        }

        return true;
    }
}

