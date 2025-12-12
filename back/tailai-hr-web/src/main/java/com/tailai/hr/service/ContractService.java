package com.tailai.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.client.FileServiceClient;
import com.tailai.common.entity.Contract;
import com.tailai.common.enums.ContractStatus;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.common.vo.ContractDetailVO;
import com.tailai.hr.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同查看服务
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;
    private final FileServiceClient fileServiceClient;

    /**
     * 查询所有合同（支持筛选）
     */
    public Result<Page<ContractDetailVO>> getAllContracts(
            int pageNum, 
            int pageSize, 
            Integer status, 
            String contractType,
            Long initiatorId) {
        
        Page<Contract> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }
        if (contractType != null && !contractType.isEmpty()) {
            wrapper.eq(Contract::getContractType, contractType);
        }
        if (initiatorId != null) {
            wrapper.eq(Contract::getInitiatorId, initiatorId);
        }
        
        wrapper.orderByDesc(Contract::getCreatedAt);
        
        Page<Contract> result = contractMapper.selectPage(page, wrapper);
        
        // 转换为VO
        Page<ContractDetailVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        List<ContractDetailVO> vos = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        
        return Result.success(voPage);
    }

    /**
     * 查询合同详情
     */
    public Result<ContractDetailVO> getContractDetail(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        ContractDetailVO vo = convertToVO(contract);

        return Result.success(vo);
    }

    /**
     * 查看指定经理下员工的合同列表
     */
    public Result<Page<ContractDetailVO>> getContractsByManager(
            Long managerId, 
            int pageNum, 
            int pageSize, 
            Integer status) {
        
        Page<Contract> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        
        // 查询该经理发起的所有合同
        wrapper.eq(Contract::getInitiatorId, managerId);
        
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }
        
        wrapper.orderByDesc(Contract::getCreatedAt);
        
        Page<Contract> result = contractMapper.selectPage(page, wrapper);
        
        // 转换为VO
        Page<ContractDetailVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        List<ContractDetailVO> vos = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        
        return Result.success(voPage);
    }
    
    /**
     * 查看指定经理下员工的已签署合同
     */
    public Result<Page<ContractDetailVO>> getSignedContractsByManager(
            Long managerId, 
            int pageNum, 
            int pageSize) {
        
        Page<Contract> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        
        // 查询该经理发起的所有已生效合同
        wrapper.eq(Contract::getInitiatorId, managerId)
               .eq(Contract::getStatus, ContractStatus.EFFECTIVE.getCode())
               .orderByDesc(Contract::getEffectiveTime);
        
        Page<Contract> result = contractMapper.selectPage(page, wrapper);
        
        // 转换为VO
        Page<ContractDetailVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        List<ContractDetailVO> vos = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(vos);
        
        return Result.success(voPage);
    }
    
    /**
     * 统计合同数据
     */
    public Result<Map<String, Object>> getStatistics() {
        // 总合同数
        Long totalCount = contractMapper.selectCount(null);
        
        // 各状态合同数
        Long pendingCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, ContractStatus.PENDING_EMPLOYEE_SIGN.getCode())
        );
        
        Long effectiveCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, ContractStatus.EFFECTIVE.getCode())
        );
        
        Long terminatedCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, ContractStatus.TERMINATED.getCode())
        );
        
        // 入职/离职数
        Long onboardCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractType, "ONBOARD")
        );
        
        Long offboardCount = contractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getContractType, "OFFBOARD")
        );

        Map<String, Object> stats = Map.of(
                "totalCount", totalCount,
                "pendingCount", pendingCount,
                "effectiveCount", effectiveCount,
                "terminatedCount", terminatedCount,
                "onboardCount", onboardCount,
                "offboardCount", offboardCount
        );

        return Result.success(stats);
    }

    /**
     * 获取合同PDF临时访问URL
     */
    public Result<String> getContractPdfUrl(Long id, String token) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }

        // HR可以查看所有合同，不需要权限验证

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
     * 转换为VO
     */
    private ContractDetailVO convertToVO(Contract contract) {
        ContractDetailVO vo = new ContractDetailVO();
        BeanUtils.copyProperties(contract, vo);
        vo.setStatusDesc(ContractStatus.fromCode(contract.getStatus()).getDesc());
        return vo;
    }
    
    /**
     * 代理下载合同文件（解决CORS问题）
     */
    public void proxyFile(String ossPath, HttpServletResponse response) {
        log.info("代理下载合同文件，OSS路径：{}", ossPath);
        
        if (ossPath == null || ossPath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }
        
        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpURLConnection connection = null;
        
        try {
            // 获取OSS临时访问URL
            Result<String> urlResult = fileServiceClient.getDownloadUrl(ossPath, 60);
            if (urlResult.getCode() != 200 || urlResult.getData() == null) {
                throw new BusinessException("获取文件URL失败");
            }
            
            String fileUrl = urlResult.getData();
            log.info("OSS文件URL：{}", fileUrl);
            
            // 建立连接
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            // 获取文件流
            inputStream = connection.getInputStream();
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            
            // 流式传输
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            
            log.info("合同文件代理下载成功");
            
        } catch (Exception e) {
            log.error("合同文件代理下载失败：{}", e.getMessage(), e);
            throw new BusinessException("合同文件代理下载失败：" + e.getMessage());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                log.error("关闭流失败", e);
            }
        }
    }
}

