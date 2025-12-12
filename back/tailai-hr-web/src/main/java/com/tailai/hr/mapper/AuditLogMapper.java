package com.tailai.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailai.common.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志Mapper
 *
 * @author Tailai
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}

