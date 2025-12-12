package com.tailai.manager.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailai.common.client.FileServiceClient;
import com.tailai.common.constant.ContractConstant;
import com.tailai.common.dto.ContractIssueRequest;
import com.tailai.common.dto.ContractSignRequest;
import com.tailai.common.entity.Contract;
import com.tailai.common.entity.ContractIssue;
import com.tailai.common.entity.ContractTemplate;
import com.tailai.common.entity.User;
import com.tailai.common.enums.ContractStatus;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.common.vo.ContractDetailVO;
import com.tailai.manager.mapper.ContractIssueMapper;
import com.tailai.manager.mapper.ContractMapper;
import com.tailai.manager.mapper.ContractTemplateMapper;
import com.tailai.manager.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同服务
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;
    private final UserMapper userMapper;
    private final ContractIssueMapper contractIssueMapper;
    private final ContractTemplateMapper contractTemplateMapper;
    private final FileServiceClient fileServiceClient;


    /**
     * 经理发起合同并签字（新流程：一步完成）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> issueContract(ContractIssueRequest request, String token) {
        // 1. 验证经理身份
        String userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        User manager = userMapper.selectById(Long.parseLong(userId));

        if (manager == null || !"MANAGER".equals(manager.getRole())) {
            throw new BusinessException("非法操作：您不是经理");
        }

        // 2. 验证员工是否存在且属于当前经理
        User employee = userMapper.selectById(request.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        
        // 验证员工角色
        if (!"EMPLOYEE".equals(employee.getRole())) {
            throw new BusinessException("该用户不是员工");
        }
        
        // 验证员工是否属于当前经理
        if (!manager.getId().equals(employee.getManagedBy())) {
            throw new BusinessException("只能给自己管理的员工下发合同");
        }

        // 3. 创建合同记录
        Contract contract = new Contract();
        contract.setContractType("ONBOARD"); // 固定为入职合同
        contract.setEmployeeId(request.getEmployeeId());
        contract.setEmployeeName(employee.getRealName());
        contract.setInitiatorId(manager.getId());
        contract.setInitiatorName(manager.getRealName());
        contract.setTemplateId(request.getTemplateId());
        contract.setPosition(request.getPosition());
        contract.setBaseSalary(request.getBaseSalary());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        
        // 生成合同编号
        contract.setContractNo(ContractConstant.CONTRACT_NO_PREFIX_ONBOARD + 
                DateUtil.format(DateUtil.date(), "yyyyMMdd") + "-" + 
                IdUtil.fastSimpleUUID().substring(0, 6));
        
        // 先插入合同，获取合同ID（用于生成PDF文件名）
        contractMapper.insert(contract);
        log.info("创建合同记录成功，合同ID：{}，合同编号：{}，员工：{}", 
                contract.getId(), contract.getContractNo(), employee.getRealName());

        // 4. 先查询模板的OSS路径
        ContractTemplate template = contractTemplateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException("合同模板不存在，模板ID：" + request.getTemplateId());
        }
        if (template.getOssFilePath() == null || template.getOssFilePath().trim().isEmpty()) {
            throw new BusinessException("合同模板文件路径为空，模板ID：" + request.getTemplateId());
        }
        
        String templateOssPath = template.getOssFilePath();
        log.info("查询到模板OSS路径：{}", templateOssPath);
        
        // 5. 生成合同PDF（从模板）
        String pdfFileName = employee.getRealName() + "_待签合同_" + contract.getId() + ".pdf";
        String pdfPath = "pending/contracts/" + pdfFileName;
        
        log.info("开始生成合同PDF，模板路径：{}，输出路径：{}", templateOssPath, pdfPath);
        
        // 准备合同数据
        Map<String, Object> contractData = new HashMap<>();
        contractData.put("contractNo", contract.getContractNo());
        contractData.put("employeeName", employee.getRealName());
        contractData.put("position", request.getPosition());
        contractData.put("baseSalary", request.getBaseSalary());
        contractData.put("startDate", request.getStartDate());
        contractData.put("endDate", request.getEndDate());
        contractData.put("managerName", manager.getRealName());
        
        // 调用文件服务生成PDF
        Result<Map<String, Object>> pdfResult = fileServiceClient.generateContractPdf(
                templateOssPath,
                pdfPath,
                contractData
        );
        
        if (pdfResult.getCode() != 200 || pdfResult.getData() == null) {
            throw new BusinessException("生成合同PDF失败：" + pdfResult.getMessage());
        }
        
        String generatedPdfPath = (String) pdfResult.getData().get("pdfPath");
        log.info("合同PDF生成成功，路径：{}", generatedPdfPath);
        
        // 6. 调用文件服务添加经理签名到PDF
        log.info("添加经理签名到PDF，合同ID：{}", contract.getId());
        Result<Map<String, Object>> signResult = fileServiceClient.addSignatureToPdf(
                generatedPdfPath,               // PDF路径（已生成）
                request.getSignatureBase64(),   // 签名Base64
                employee.getRealName(),         // 员工姓名
                manager.getRealName(),          // 签名人姓名
                "MANAGER"                       // 签名人角色
        );
        
        if (signResult.getCode() != 200 || signResult.getData() == null) {
            throw new BusinessException("签名处理失败：" + signResult.getMessage());
        }
        
        // 更新合同PDF路径为签名后的文件路径
        String signedPdfPath = (String) signResult.getData().get("signedPdfPath");
        contract.setContractPdfUrl(signedPdfPath);
        log.info("经理签名后PDF路径：{}", signedPdfPath);

        // 7. 更新合同状态为"待员工签字"
        contract.setStatus(ContractStatus.PENDING_EMPLOYEE_SIGN.getCode());
        contract.setInitiatorSignTime(LocalDateTime.now());
        contract.setIssueTime(LocalDateTime.now());
        contractMapper.updateById(contract);
        log.info("合同状态更新为待员工签字");

        // 8. 创建下发记录
        ContractIssue issue = new ContractIssue();
        issue.setContractId(contract.getId());
        issue.setManagerId(manager.getId());
        issue.setEmployeeId(request.getEmployeeId());
        issue.setIssueTime(LocalDateTime.now());
        issue.setStatus(1); // 已下发待签
        issue.setNotificationSent(request.getSendNotification() ? 1 : 0);
        contractIssueMapper.insert(issue);
        log.info("创建下发记录成功，合同ID：{}，员工ID：{}", contract.getId(), request.getEmployeeId());

        // TODO: 发送微信订阅消息通知员工

        return Result.success(contract.getId());
    }
    
    /**
     * 经理签署合同（已废弃，新流程中签署和下发合并）
     * @deprecated 使用 issueContract 代替
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> signContract(ContractSignRequest request, String token) {
        throw new BusinessException("此接口已废弃，请使用发起合同接口（发起时直接签字）");
    }

    /**
     * 查询我发起的合同列表
     */
    public Result<List<ContractDetailVO>> getMyContracts(String token, Integer status) {
        String userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getInitiatorId, Long.parseLong(userId));
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }
        wrapper.orderByDesc(Contract::getCreatedAt);

        List<Contract> contracts = contractMapper.selectList(wrapper);
        
        List<ContractDetailVO> vos = contracts.stream().map(this::convertToVO).collect(Collectors.toList());
        
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

        // TODO: 权限验证

        ContractDetailVO vo = convertToVO(contract);
        return Result.success(vo);
    }

    /**
     * 查询合同状态
     */
    public Result<String> getContractStatus(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        String statusDesc = ContractStatus.fromCode(contract.getStatus()).getDesc();
        return Result.success(statusDesc);
    }

    /**
     * 获取可用的合同模板列表
     */
    public Result<List<ContractTemplate>> getAvailableTemplates() {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractTemplate::getStatus, 1) // 只查询启用的模板
                .orderByDesc(ContractTemplate::getCreatedAt);
        
        List<ContractTemplate> templates = contractTemplateMapper.selectList(wrapper);
        log.info("查询到 {} 个可用模板", templates.size());
        
        return Result.success(templates);
    }

    /**
     * 获取合同PDF临时访问URL
     */
    public Result<String> getContractPdfUrl(Long id, String token) {
        String userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 验证权限：只能查看自己发起的合同
        if (!contract.getInitiatorId().equals(Long.parseLong(userId))) {
            throw new BusinessException("无权限查看此合同");
        }

        if (contract.getContractPdfUrl() == null || contract.getContractPdfUrl().trim().isEmpty()) {
            throw new BusinessException("合同PDF不存在");
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
     * 终止已生效的合同
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> terminateContract(Long id, String token) {
        String userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        
        // 1. 查询合同
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // 2. 验证权限：只能终止自己发起的合同
        if (!contract.getInitiatorId().equals(Long.parseLong(userId))) {
            throw new BusinessException("无权限终止此合同");
        }

        // 3. 验证合同状态：只能终止已生效的合同
        if (!ContractStatus.EFFECTIVE.getCode().equals(contract.getStatus())) {
            String statusDesc = ContractStatus.fromCode(contract.getStatus()).getDesc();
            throw new BusinessException("只能终止已生效的合同，当前合同状态：" + statusDesc);
        }

        // 4. 更新合同状态为已终止
        contract.setStatus(ContractStatus.TERMINATED.getCode());
        contractMapper.updateById(contract);
        
        log.info("合同{}已被终止，操作人ID：{}", id, userId);

        // TODO: 发送通知给员工（可选）

        return Result.success();
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

