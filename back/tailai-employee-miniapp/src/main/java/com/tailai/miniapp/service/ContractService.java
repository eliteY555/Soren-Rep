package com.tailai.miniapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.client.FileServiceClient;
import com.tailai.common.dto.ContractSignRequest;
import com.tailai.common.entity.Contract;
import com.tailai.common.entity.User;
import com.tailai.common.enums.ContractStatus;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.common.vo.ContractDetailVO;
import com.tailai.miniapp.mapper.ContractMapper;
import com.tailai.miniapp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同服务（新流程）
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;
    private final UserMapper userMapper;
    private final FileServiceClient fileServiceClient;

    /**
     * 查询待签合同列表
     */
    public Result<List<ContractDetailVO>> getPendingContracts(String token) {
        Long userId = JwtUtil.getUserIdAsLong(token.replace("Bearer ", ""));

        // 查询下发给我的待签合同
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getEmployeeId, userId)
                .eq(Contract::getStatus, ContractStatus.PENDING_EMPLOYEE_SIGN.getCode())
                .orderByDesc(Contract::getIssueTime);

        List<Contract> contracts = contractMapper.selectList(wrapper);
        List<ContractDetailVO> vos = contracts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    /**
     * 查询我的合同列表
     */
    public Result<List<ContractDetailVO>> getMyContracts(String token, Integer status) {
        Long userId = JwtUtil.getUserIdAsLong(token.replace("Bearer ", ""));

        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getEmployeeId, userId);
        
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        } else {
            // 默认只查看已生效和已终止的合同
            wrapper.in(Contract::getStatus, 
                    ContractStatus.EFFECTIVE.getCode(), 
                    ContractStatus.TERMINATED.getCode());
        }
        
        wrapper.orderByDesc(Contract::getCreatedAt);

        List<Contract> contracts = contractMapper.selectList(wrapper);
        List<ContractDetailVO> vos = contracts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    /**
     * 查询合同详情
     */
    public Result<ContractDetailVO> getContractDetail(Long id, String token) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 验证权限：只能查看自己的合同
        Long userId = JwtUtil.getUserIdAsLong(token.replace("Bearer ", ""));
        if (!contract.getEmployeeId().equals(userId)) {
            throw new BusinessException("无权限查看该合同");
        }

        ContractDetailVO vo = convertToVO(contract);

        return Result.success(vo);
    }

    /**
     * 员工签署合同
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> signContract(ContractSignRequest request, String token) {
        Long userId = JwtUtil.getUserIdAsLong(token.replace("Bearer ", ""));
        User employee = userMapper.selectById(userId);

        // 查询合同
        Contract contract = contractMapper.selectById(request.getContractId());
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 验证是否是本人的合同
        if (!contract.getEmployeeId().equals(employee.getId())) {
            throw new BusinessException("只能签署下发给自己的合同");
        }

        // 验证合同状态
        if (!ContractStatus.PENDING_EMPLOYEE_SIGN.getCode().equals(contract.getStatus())) {
            throw new BusinessException("合同状态不正确，无法签署");
        }

        // 调用文件服务将员工签名叠加到PDF
        log.info("调用文件服务添加员工签名到PDF，合同ID：{}", request.getContractId());
        Result<Map<String, Object>> signResult = fileServiceClient.addSignatureToPdf(
                contract.getContractPdfUrl(),  // 经理签名后的PDF路径（temp-contracts目录）
                request.getSignatureBase64(),   // 员工签名Base64
                contract.getEmployeeName(),     // 员工姓名
                employee.getRealName(),          // 签名人姓名
                "EMPLOYEE"                       // 签名人角色
        );
        
        if (signResult.getCode() != 200 || signResult.getData() == null) {
            throw new BusinessException("签名处理失败：" + signResult.getMessage());
        }
        
        // 更新合同PDF路径为员工签名后的文件路径（effective-contracts目录）
        String signedPdfPath = (String) signResult.getData().get("signedPdfPath");
        contract.setContractPdfUrl(signedPdfPath);
        log.info("员工签名后PDF路径（合同已生效）：{}", signedPdfPath);

        // 更新合同状态为已生效（员工签字后合同立即生效）
        contract.setStatus(ContractStatus.EFFECTIVE.getCode());
        contract.setEmployeeSignTime(LocalDateTime.now());
        contract.setEffectiveTime(LocalDateTime.now()); // 生效时间=员工签字时间
        contractMapper.updateById(contract);

        log.info("员工{}签署合同{}成功，合同已生效", employee.getId(), request.getContractId());
        // TODO: 发送通知给经理：员工已完成签署，合同已生效

        return Result.success();
    }

    /**
     * 拒绝签署合同
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> rejectContract(Long contractId, String reason, String token) {
        Long userId = JwtUtil.getUserIdAsLong(token.replace("Bearer ", ""));

        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 验证权限
        if (!contract.getEmployeeId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }

        // 验证状态
        if (!ContractStatus.PENDING_EMPLOYEE_SIGN.getCode().equals(contract.getStatus())) {
            throw new BusinessException("合同状态不正确");
        }

        // 更新合同状态（可以设计一个REJECTED拒签状态）
        // 这里暂时保持状态，记录拒签原因
        // TODO: 可以新增一个reject_reason字段

        // TODO: 通知经理：员工已拒签

        return Result.success();
    }

    /**
     * 获取合同PDF临时访问URL
     */
    public Result<String> getContractPdfUrl(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 调用文件服务获取临时访问URL（60分钟有效期）
        log.info("获取合同{}的PDF临时访问URL", id);
        Result<String> urlResult = fileServiceClient.getDownloadUrl(contract.getContractPdfUrl(), 60);
        
        if (urlResult.getCode() != 200 || urlResult.getData() == null) {
            throw new BusinessException("获取PDF访问URL失败：" + urlResult.getMessage());
        }

        return Result.success(urlResult.getData());
    }

    /**
     * 转换为VO
     */
    private ContractDetailVO convertToVO(Contract contract) {
        ContractDetailVO vo = new ContractDetailVO();
        BeanUtils.copyProperties(contract, vo);
        vo.setStatusDesc(ContractStatus.fromCode(contract.getStatus()).getDesc());
        return vo;
    }
}







