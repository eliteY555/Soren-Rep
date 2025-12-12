package com.tailai.file.controller;

import com.tailai.common.result.Result;
import com.tailai.file.dto.SignatureRequest;
import com.tailai.file.service.SignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 电子签名管理接口
 *
 * @author Tailai
 */
@Slf4j
@RestController
@RequestMapping("/api/file/signature")
@Tag(name = "电子签名管理", description = "电子签名处理、签名叠加到PDF")
public class SignatureController {

    @Autowired
    private SignatureService signatureService;

    /**
     * 添加签名到PDF
     */
    @PostMapping("/add")
    @Operation(summary = "添加签名到PDF", description = "将电子签名叠加到PDF文件的指定位置")
    public Result<Map<String, Object>> addSignatureToPdf(@RequestBody SignatureRequest request) {
        log.info("========== 接收签名请求 ==========");
        log.info("PDF路径：{}", request.getPdfPath());
        log.info("员工姓名：{}", request.getEmployeeName());
        log.info("签名人：{} ({})", request.getSignerName(), request.getSignerRole());

        try {
            Map<String, Object> result = signatureService.addSignatureToPdf(request);
            
            log.info("========== 签名处理完成 ==========");
            log.info("签名后PDF：{}", result.get("signedPdfPath"));
            
            return Result.success("签名添加成功", result);
            
        } catch (Exception e) {
            log.error("签名处理失败", e);
            return Result.error("签名处理失败：" + e.getMessage());
        }
    }
}

