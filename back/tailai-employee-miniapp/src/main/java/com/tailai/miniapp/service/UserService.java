package com.tailai.miniapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.entity.User;
import com.tailai.common.exception.BusinessException;
import com.tailai.miniapp.dto.ChangePasswordRequest;
import com.tailai.miniapp.dto.UpdateUserInfoRequest;
import com.tailai.miniapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户信息服务
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 更新用户信息
     *
     * @param userId  用户ID
     * @param request 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        // 1. 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 2. 检查手机号是否被其他用户使用
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(User::getPhone, request.getPhone())
                      .ne(User::getId, userId);
            Long phoneCount = userMapper.selectCount(phoneQuery);
            if (phoneCount > 0) {
                throw new BusinessException(400, "该手机号已被其他用户使用");
            }
        }

        // 3. 更新用户信息
        User updateUser = new User();
        updateUser.setId(userId);
        
        if (request.getRealName() != null) {
            updateUser.setRealName(request.getRealName().trim());
        }
        if (request.getPhone() != null) {
            updateUser.setPhone(request.getPhone().trim());
        }
        if (request.getIdCard() != null) {
            updateUser.setIdCard(request.getIdCard().trim().toUpperCase());
        }
        if (request.getGender() != null) {
            updateUser.setGender(request.getGender());
        }

        userMapper.updateById(updateUser);
        log.info("用户信息更新成功，userId: {}", userId);
    }

    /**
     * 修改密码
     *
     * @param userId  用户ID
     * @param request 修改密码请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 2. 验证旧密码（注意：这里是明文密码比对，生产环境应使用BCrypt）
        if (!request.getOldPassword().equals(user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        // 3. 检查新旧密码不能相同
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }

        // 4. 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(request.getNewPassword());
        userMapper.updateById(updateUser);

        log.info("用户密码修改成功，userId: {}", userId);
    }

    /**
     * 上传头像（已废弃，因为不再使用头像功能）
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像URL
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        throw new BusinessException(400, "头像功能已废弃");
    }
}




