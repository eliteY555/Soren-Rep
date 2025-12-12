package com.tailai.hr.controller;

import com.tailai.common.result.Result;
import com.tailai.common.client.FileServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文件服务代理控制器
 * 代理文件服务的接口，供前端调用
 *
 * @author Tailai
 */
@Slf4j
@Tag(name = "文件代理", description = "代理文件服务接口")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileProxyController {

    private final FileServiceClient fileServiceClient;

    /**
     * 获取文件临时访问URL
     */
    @GetMapping("/download-url")
    @Operation(summary = "获取文件临时访问URL", description = "根据OSS路径获取临时访问URL")
    public Result<String> getDownloadUrl(
            @Parameter(description = "OSS文件路径", required = true)
            @RequestParam("ossPath") String ossPath,
            
            @Parameter(description = "过期时间（分钟）", example = "60")
            @RequestParam(value = "expireMinutes", required = false, defaultValue = "60") Integer expireMinutes) {
        
        log.info("代理请求：获取文件临时URL，OSS路径：{}，过期时间：{}分钟", ossPath, expireMinutes);
        
        return fileServiceClient.getDownloadUrl(ossPath, expireMinutes);
    }
}

