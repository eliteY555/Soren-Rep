package com.tailai.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.result.Result;
import com.tailai.common.vo.ContractDetailVO;
import com.tailai.hr.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 合同管理控制器
 *
 * @author Tailai
 */
@Slf4j
@Tag(name = "合同查看", description = "人事查看合同接口")
@RestController
@RequestMapping("/api/hr/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    
    @Operation(summary = "查看指定经理下员工的合同列表", 
               description = "查看某个经理管理的所有员工的合同，支持预览PDF文件。需要Authorization请求头")
    @GetMapping("/by-manager/{managerId}")
    public Result<Page<ContractDetailVO>> getContractsByManager(
            @PathVariable Long managerId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "合同状态：1-待员工签字，2-已生效，3-已终止") 
            @RequestParam(required = false) Integer status,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return contractService.getContractsByManager(managerId, pageNum, pageSize, status);
    }
    
    @Operation(summary = "代理下载合同文件", description = "通过后端代理下载合同文件，解决CORS问题，支持PDF预览。需要Authorization请求头")
    @GetMapping("/proxy-file")
    public void proxyFile(
            @Parameter(description = "OSS文件路径", required = true) @RequestParam String ossPath,
            javax.servlet.http.HttpServletResponse response,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        log.info("收到合同文件代理请求，ossPath: {}", ossPath);
        contractService.proxyFile(ossPath, response);
    }

    @Operation(summary = "获取合同PDF临时访问URL", description = "用于预览和下载合同")
    @GetMapping("/pdf-url/{id}")
    public Result<String> getContractPdfUrl(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return contractService.getContractPdfUrl(id, token);
    }
}

