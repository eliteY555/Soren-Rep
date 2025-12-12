package com.tailai.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailai.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 *
 * @author Tailai
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

