package com.tailai.miniapp.controller;

import com.tailai.common.dto.ContractSignRequest;
import com.tailai.common.result.Result;
import com.tailai.common.vo.ContractDetailVO;
import com.tailai.miniapp.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同管理控制器（新流程）
 *
 * @author Tailai
 */
@Tag(name = "合同管理", description = "待签合同、合同签署、合同查看")
@RestController
@RequestMapping("/api/miniapp/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "待签合同列表", description = "获取下发给我的待签合同")
    @GetMapping("/pending")
    public Result<List<ContractDetailVO>> getPendingContracts(
            @RequestHeader("Authorization") String token) {
        return contractService.getPendingContracts(token);
    }

    @Operation(summary = "我的合同列表", description = "查看我的所有合同")
    @GetMapping("/my-list")
    public Result<List<ContractDetailVO>> getMyContracts(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "合同状态") @RequestParam(required = false) Integer status) {
        return contractService.getMyContracts(token, status);
    }

    @Operation(summary = "合同详情", description = "查看合同详细信息")
    @GetMapping("/detail/{id}")
    public Result<ContractDetailVO> getContractDetail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        return contractService.getContractDetail(id, token);
    }

    @Operation(summary = "员工签署合同", description = "员工电子签名确认合同")
    @PostMapping("/sign")
    public Result<Void> signContract(
            @Validated @RequestBody ContractSignRequest request,
            @RequestHeader("Authorization") String token) {
        return contractService.signContract(request, token);
    }

    @Operation(summary = "拒绝签署合同", description = "员工拒绝签署并填写原因")
    @PostMapping("/reject")
    public Result<Void> rejectContract(
            @RequestParam Long contractId,
            @RequestParam String reason,
            @RequestHeader("Authorization") String token) {
        return contractService.rejectContract(contractId, reason, token);
    }

    @Operation(summary = "获取合同PDF临时访问URL", description = "用于预览和下载合同")
    @GetMapping("/pdf-url/{id}")
    public Result<String> getContractPdfUrl(@PathVariable Long id) {
        return contractService.getContractPdfUrl(id);
    }
}







