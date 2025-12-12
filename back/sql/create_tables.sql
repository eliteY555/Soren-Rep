-- ========================================
-- 泰来云员工管理系统 - 数据库表结构创建脚本
-- ========================================

-- 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS `tailai_admin`;

-- 创建数据库
CREATE DATABASE `tailai_admin` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_general_ci;

-- 使用数据库
USE `tailai_admin`;

-- 1. 用户表（使用姓名/手机号+密码登录）
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密存储）',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名（必填）',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号（必填，唯一，用作登录账号）',
    `id_card` VARCHAR(100) COMMENT '身份证号（加密存储，唯一）',
    `gender` TINYINT COMMENT '性别：0-女，1-男',
    `role` VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '用户角色：EMPLOYEE-员工，MANAGER-经理，HR-人事',
    `managed_by` BIGINT COMMENT '所属经理ID（员工专用，外键关联user.id）',
    `department_id` BIGINT COMMENT '所属部门ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-冻结，1-正常',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_id_card` (`id_card`),
    INDEX `idx_role` (`role`),
    INDEX `idx_managed_by` (`managed_by`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（员工、经理、HR统一管理）';

-- 2. 合同模板表
CREATE TABLE `contract_template` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_code` VARCHAR(50) UNIQUE NOT NULL COMMENT '模板编码',
    `template_type` VARCHAR(20) NOT NULL COMMENT '模板类型：ONBOARD-入职，OFFBOARD-离职',
    `contract_category` VARCHAR(20) COMMENT '合同分类：FORMAL-正式工，TEMP-临时工，INTERN-实习生',
    `template_content` LONGTEXT COMMENT 'HTML格式的模板内容',
    `oss_file_path` VARCHAR(500) COMMENT 'OSS存储路径',
    `variables_config` TEXT COMMENT 'JSON格式的变量配置',
    `signature_positions` TEXT COMMENT 'JSON格式的签名位置配置',
    `version` VARCHAR(20) DEFAULT '1.0' COMMENT '版本号',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) COMMENT '备注说明',
    `created_by` BIGINT COMMENT '创建人ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_template_code` (`template_code`),
    INDEX `idx_template_type` (`template_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板表';

-- 3. 合同表（优化后：增加拒签原因字段）
CREATE TABLE `contract` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `contract_no` VARCHAR(50) UNIQUE NOT NULL COMMENT '合同编号（自动生成）',
    `contract_type` VARCHAR(20) NOT NULL COMMENT '合同类型：ONBOARD-入职，OFFBOARD-离职',
    `employee_id` BIGINT NOT NULL COMMENT '员工ID（外键关联user.id）',
    `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
    `initiator_id` BIGINT NOT NULL COMMENT '发起人ID（经理ID，外键关联user.id）',
    `initiator_name` VARCHAR(50) NOT NULL COMMENT '发起人姓名',
    `template_id` BIGINT COMMENT '使用的模板ID（外键关联contract_template.id）',
    `position` VARCHAR(50) COMMENT '岗位',
    `base_salary` DECIMAL(10,2) COMMENT '基本工资',
    `start_date` DATE COMMENT '合同开始日期',
    `end_date` DATE COMMENT '合同结束日期',
    `contract_pdf_url` VARCHAR(500) COMMENT '合同PDF存储路径（OSS路径）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-待员工签字，2-已生效，3-已终止',
    `reject_reason` VARCHAR(500) COMMENT '拒签原因（员工拒签时填写）',
    `initiator_sign_time` DATETIME COMMENT '经理签署时间',
    `issue_time` DATETIME COMMENT '下发时间',
    `employee_sign_time` DATETIME COMMENT '员工签署时间',
    `effective_time` DATETIME COMMENT '合同生效时间（员工签字后立即生效）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_contract_no` (`contract_no`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_initiator_id` (`initiator_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_contract_type` (`contract_type`),
    INDEX `idx_effective_time` (`effective_time`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同表（入职/离职合同统一管理）';

-- 4. 合同下发记录表
CREATE TABLE `contract_issue` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `contract_id` BIGINT NOT NULL COMMENT '合同ID',
    `manager_id` BIGINT NOT NULL COMMENT '下发经理ID',
    `employee_id` BIGINT NOT NULL COMMENT '目标员工ID',
    `issue_time` DATETIME NOT NULL COMMENT '下发时间',
    `employee_sign_time` DATETIME COMMENT '员工签署时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-已下发待签，2-员工已签',
    `notification_sent` TINYINT DEFAULT 0 COMMENT '是否已发送通知：0-未发送，1-已发送',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_contract_id` (`contract_id`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_manager_id` (`manager_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同下发记录表';

-- 5. 经理权限配置表
CREATE TABLE `manager_permission` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `manager_id` BIGINT NOT NULL COMMENT '经理ID',
    `department_id` BIGINT COMMENT '可管理的部门ID（NULL表示不限制）',
    `can_issue_contract` TINYINT DEFAULT 1 COMMENT '是否可发起合同：0-否，1-是',
    `can_manage_employee` TINYINT DEFAULT 1 COMMENT '是否可管理员工：0-否，1-是',
    `created_by` BIGINT COMMENT '创建人（人事ID）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_manager_id` (`manager_id`),
    INDEX `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='经理权限配置表';

-- 6. 系统审计日志表
CREATE TABLE `audit_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '操作人ID',
    `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型：EMPLOYEE/MANAGER/HR',
    `operation` VARCHAR(50) NOT NULL COMMENT '操作类型：LOGIN/CREATE_CONTRACT/SIGN/ISSUE/APPROVE等',
    `resource_type` VARCHAR(50) COMMENT '资源类型：CONTRACT/TEMPLATE/USER等',
    `resource_id` BIGINT COMMENT '资源ID',
    `operation_detail` TEXT COMMENT '操作详情（JSON格式）',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_type` (`user_type`),
    INDEX `idx_operation` (`operation`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统审计日志表';

-- 表创建完成
-- 注意：请执行 insert_data.sql 插入初始数据
