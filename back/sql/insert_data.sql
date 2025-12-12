-- ========================================
-- 泰来云员工管理系统 - 初始数据插入脚本
-- 使用姓名/手机号+密码登录，统一密码为123456
-- ========================================

-- 1. 插入默认人事账号
-- 手机号: 13800138000, 密码: 123456 (BCrypt加密后)
INSERT INTO `user` (`password`, `real_name`, `phone`, `id_card`, `role`, `status`) VALUES
('$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '系统管理员', '13800138000', '110101199001011234', 'HR', 1);

-- 2. 插入默认经理账号
-- 手机号: 13800138001, 密码: 123456 (BCrypt加密后)
-- 手机号: 13800138002, 密码: 123456 (BCrypt加密后)
INSERT INTO `user` (`password`, `real_name`, `phone`, `id_card`, `role`, `department_id`, `status`) VALUES
('$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '张经理', '13800138001', '110101198501011234', 'MANAGER', 1, 1),
('$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '李经理', '13800138002', '110101198601011234', 'MANAGER', 2, 1);

-- 3. 插入测试员工（使用手机号和密码登录）
-- 密码: 123456 (BCrypt加密后)
INSERT INTO `user` (`password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `managed_by`, `status`) VALUES
('$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '测试员工01', '13900139001', '110101199501011001', 1, 'EMPLOYEE', 2, 1),
('$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '测试员工02', '13900139002', '110101199601011002', 0, 'EMPLOYEE', 2, 1),
('$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '测试员工03', '13900139003', '110101199701011003', 1, 'EMPLOYEE', 3, 1);

-- 说明：
-- 1. 所有用户统一使用姓名/手机号作为登录账号，密码均为：123456
-- 2. role字段表示用户角色：HR-人事、MANAGER-经理、EMPLOYEE-员工
-- 3. 测试员工01、02属于张经理(ID=2)，测试员工03属于李经理(ID=3)
-- 4. BCrypt加密后的值：$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.
