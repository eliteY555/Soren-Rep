package com.tailai.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.entity.ContractTemplate;
import com.tailai.common.result.Result;
import com.tailai.common.util.JwtUtil;
import com.tailai.hr.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 模板管理控制器
 *
 * @author Tailai
 */
@Slf4j
@Tag(name = "模板管理", description = "合同模板管理接口")
@RestController
@RequestMapping("/api/hr/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "上传三方合同模板", description = "上传三方合同模板文件，自动创建或更新模板记录。需要Authorization请求头")
    @PostMapping("/upload")
    public Result<Long> uploadTemplate(
            @Parameter(description = "模板文件（PDF格式）", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        
        String userId = JwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        // 固定参数：三方合同模板
        return templateService.uploadTemplate(file, "三方合同模板", "TRIPARTITE_CONTRACT", 
                "ONBOARD", "FORMAL", null, Long.parseLong(userId));
    }
    
    @Operation(summary = "模板列表", description = "分页查询模板列表。需要Authorization请求头")
    @GetMapping("/list")
    public Result<Page<ContractTemplate>> getTemplateList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "模板类型") @RequestParam(required = false) String templateType,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return templateService.getTemplateList(pageNum, pageSize, templateType);
    }

    @Operation(summary = "模板详情", description = "查看模板详细信息。需要Authorization请求头")
    @GetMapping("/detail/{id}")
    public Result<ContractTemplate> getTemplateDetail(
            @PathVariable Long id,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return templateService.getTemplateDetail(id);
    }

    @Operation(summary = "删除模板", description = "删除模板记录和OSS文件。需要Authorization请求头")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteTemplate(
            @PathVariable Long id,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return templateService.deleteTemplate(id);
    }
    
    @Operation(summary = "代理下载模板文件", description = "通过后端代理下载模板文件，解决CORS问题，支持PDF预览。需要Authorization请求头")
    @GetMapping("/proxy-file")
    public void proxyFile(
            @Parameter(description = "OSS文件路径", required = true) @RequestParam String ossPath,
            javax.servlet.http.HttpServletResponse response,
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        log.info("收到文件代理请求，ossPath: {}", ossPath);
        templateService.proxyFile(ossPath, response);
    }
}

