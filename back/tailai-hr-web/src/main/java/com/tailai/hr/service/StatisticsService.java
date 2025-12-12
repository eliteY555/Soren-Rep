package com.tailai.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.entity.Contract;
import com.tailai.common.entity.User;
import com.tailai.common.result.Result;
import com.tailai.hr.mapper.ContractMapper;
import com.tailai.hr.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务
 *
 * @author Tailai
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ContractMapper contractMapper;
    private final UserMapper userMapper;

    /**
     * 获取数据概览
     */
    public Result<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 合同总数
        Long totalContracts = contractMapper.selectCount(null);
        dashboard.put("totalContracts", totalContracts);

        // 本月入职人数
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        Long monthOnboard = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractType, "ONBOARD")
                        .eq(Contract::getStatus, 2) // 已生效
                        .ge(Contract::getEffectiveTime, monthStart)
        );
        dashboard.put("monthOnboard", monthOnboard);

        // 本月离职人数
        Long monthOffboard = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractType, "OFFBOARD")
                        .eq(Contract::getStatus, 2) // 已生效
                        .ge(Contract::getEffectiveTime, monthStart)
        );
        dashboard.put("monthOffboard", monthOffboard);

        // 待签署合同数
        Long pendingSign = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, 1) // 待员工签字
        );
        dashboard.put("pendingSign", pendingSign);

        // 在职员工总数
        Long activeEmployees = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "EMPLOYEE")
                        .eq(User::getStatus, 1)
        );
        dashboard.put("activeEmployees", activeEmployees);

        // 经理总数
        Long managerCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "MANAGER")
                        .eq(User::getStatus, 1)
        );
        dashboard.put("managerCount", managerCount);

        return Result.success(dashboard);
    }

    /**
     * 获取入离职趋势（最近12个月）
     */
    public Result<Map<String, Object>> getTrend() {
        Map<String, Object> trend = new HashMap<>();
        
        // TODO: 实现按月统计入离职趋势
        // 查询最近12个月的入职和离职数据
        // 按月份分组统计
        
        return Result.success(trend);
    }

    /**
     * 获取部门分布
     */
    public Result<Map<String, Object>> getDepartmentDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        
        // TODO: 实现部门人员分布统计
        // 按部门统计在职人数
        
        return Result.success(distribution);
    }

    /**
     * 获取经理工作量统计
     */
    public Result<List<Map<String, Object>>> getManagerWorkload() {
        // TODO: 统计每个经理发起的合同数量
        
        return Result.success();
    }
}

