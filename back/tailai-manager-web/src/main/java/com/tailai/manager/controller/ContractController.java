package com.tailai.manager.controller;

import com.tailai.common.dto.ContractIssueRequest;
import com.tailai.common.dto.ContractSignRequest;
import com.tailai.common.entity.ContractTemplate;
import com.tailai.common.result.Result;
import com.tailai.common.vo.ContractDetailVO;
import com.tailai.manager.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同管理控制器
 *
 * @author Tailai
 */
@Tag(name = "合同管理", description = "经理端合同相关接口")
@RestController
@RequestMapping("/api/manager/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "下发合同给员工", description = "经理下发合同模板给员工，创建合同记录并通知员工")
    @PostMapping("/issue")
    public Result<Long> issueContract(
            @Validated @RequestBody ContractIssueRequest request,
            @RequestHeader("Authorization") String token) {
        return contractService.issueContract(request, token);
    }

    @Operation(summary = "经理签署合同", description = "经理对合同进行电子签名")
    @PostMapping("/sign")
    public Result<Void> signContract(
            @Validated @RequestBody ContractSignRequest request,
            @RequestHeader("Authorization") String token) {
        return contractService.signContract(request, token);
    }

    @Operation(summary = "我发起的合同列表", description = "查询当前经理发起的所有合同")
    @GetMapping("/my-list")
    public Result<List<ContractDetailVO>> getMyContracts(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "合同状态，不传查全部") @RequestParam(required = false) Integer status) {
        return contractService.getMyContracts(token, status);
    }

    @Operation(summary = "合同详情", description = "查看合同详细信息")
    @GetMapping("/detail/{id}")
    public Result<ContractDetailVO> getContractDetail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return contractService.getContractDetail(id, token);
    }

    @Operation(summary = "查看合同签署状态", description = "查看员工是否已签署")
    @GetMapping("/status/{id}")
    public Result<String> getContractStatus(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return contractService.getContractStatus(id);
    }

    @Operation(summary = "获取可用合同模板", description = "查询所有启用状态的合同模板")
    @GetMapping("/templates")
    public Result<List<ContractTemplate>> getAvailableTemplates() {
        return contractService.getAvailableTemplates();
    }

    @Operation(summary = "获取合同PDF临时访问URL", description = "用于预览和下载合同")
    @GetMapping("/pdf-url/{id}")
    public Result<String> getContractPdfUrl(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return contractService.getContractPdfUrl(id, token);
    }

    @Operation(summary = "终止已生效的合同", description = "经理终止已生效的合同")
    @PostMapping("/terminate/{id}")
    public Result<Void> terminateContract(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return contractService.terminateContract(id, token);
    }
}

