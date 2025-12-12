package com.tailai.file.controller;

import com.tailai.common.result.Result;
import com.tailai.file.dto.FileUploadResponse;
import com.tailai.file.service.ContractPdfService;
import com.tailai.file.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件管理接口
 *
 * @author Tailai
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件管理", description = "文件上传下载、OSS管理")
public class FileController {

    @Autowired
    private OssService ossService;
    
    @Autowired
    private ContractPdfService contractPdfService;

    /**
     * 通用文件上传
     */
    @PostMapping("/upload")
    @Operation(summary = "通用文件上传", description = "上传文件到OSS，支持所有文件类型")
    public Result<FileUploadResponse> uploadFile(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "存储目录（如：templates/contracts/）")
            @RequestParam(value = "directory", required = false, defaultValue = "files") String directory,

            @Parameter(description = "是否使用日期子目录（yyyy/MM/dd）")
            @RequestParam(value = "useDateSubDir", required = false, defaultValue = "true") Boolean useDateSubDir,

            @Parameter(description = "是否使用UUID文件名")
            @RequestParam(value = "useUuidFileName", required = false, defaultValue = "true") Boolean useUuidFileName
    ) {
        log.info("收到文件上传请求，文件名：{}，目录：{}，使用日期子目录：{}，使用UUID文件名：{}", 
                file.getOriginalFilename(), directory, useDateSubDir, useUuidFileName);

        FileUploadResponse response = ossService.uploadFile(file, directory, useDateSubDir, useUuidFileName, null);

        return Result.success("文件上传成功", response);
    }

    /**
     * 自定义路径文件上传
     */
    @PostMapping("/upload-custom")
    @Operation(summary = "自定义路径文件上传", description = "上传文件到OSS，支持完全自定义存储路径")
    public Result<FileUploadResponse> uploadFileCustomPath(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "完整自定义路径（如：my/project/documents/report.pdf）", required = true)
            @RequestParam("customPath") String customPath
    ) {
        log.info("收到自定义路径文件上传请求，文件名：{}，自定义路径：{}", 
                file.getOriginalFilename(), customPath);

        FileUploadResponse response = ossService.uploadFileWithCustomPath(file, customPath, true);

        return Result.success("文件上传成功", response);
    }

    /**
     * 合同模板上传
     */
    @PostMapping("/template/upload")
    @Operation(summary = "合同模板上传", description = "上传合同模板文件（仅支持PDF）到OSS")
    public Result<FileUploadResponse> uploadTemplate(
            @Parameter(description = "模板文件（PDF格式）", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        log.info("收到合同模板上传请求，文件名：{}，ContentType：{}", 
                file.getOriginalFilename(), file.getContentType());

        // 验证文件不为空
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        // 验证文件类型：只允许PDF
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            return Result.error(400, "只支持上传PDF文件，请选择.pdf格式的文件");
        }
        
        if (contentType == null || !contentType.equals("application/pdf")) {
            return Result.error(400, "文件类型错误，只支持PDF格式（application/pdf）");
        }

        // 验证文件大小（限制20MB）
        long maxSize = 20 * 1024 * 1024; // 20MB
        if (file.getSize() > maxSize) {
            return Result.error(400, "文件大小不能超过20MB");
        }

        log.info("文件验证通过，开始上传到OSS");
        // 上传到固定路径 templates/contracts，不使用日期子目录
        FileUploadResponse response = ossService.uploadFile(file, "templates/contracts", false, true, null);

        return Result.success("模板上传成功", response);
    }

    /**
     * 从模板生成合同PDF
     */
    @PostMapping("/contract/generate-pdf")
    @Operation(summary = "生成合同PDF", description = "从模板生成合同PDF文件")
    public Result<Map<String, Object>> generateContractPdf(
            @RequestBody Map<String, Object> request
    ) {
        log.info("收到生成合同PDF请求：{}", request);
        
        String templateOssPath = (String) request.get("templateOssPath");
        String outputPath = (String) request.get("outputPath");
        @SuppressWarnings("unchecked")
        Map<String, Object> contractData = (Map<String, Object>) request.get("contractData");
        
        Map<String, Object> result = contractPdfService.generateContractPdfFromTemplate(
                templateOssPath, contractData, outputPath);
        
        return Result.success("合同PDF生成成功", result);
    }

    /**
     * 证件图片上传
     */
    @PostMapping("/image/upload")
    @Operation(summary = "证件图片上传", description = "上传证件照片（身份证、学历证书等）")
    public Result<FileUploadResponse> uploadImage(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "图片类型（id-card/diploma/other）")
            @RequestParam(value = "imageType", required = false, defaultValue = "other") String imageType
    ) {
        log.info("收到图片上传请求，文件名：{}，类型：{}", file.getOriginalFilename(), imageType);

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只支持图片文件");
        }

        // 验证文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过10MB");
        }

        String directory = "certificates/" + imageType;
        FileUploadResponse response = ossService.uploadFile(file, directory);

        return Result.success("图片上传成功", response);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除文件", description = "根据OSS路径删除文件")
    public Result<String> deleteFile(
            @Parameter(description = "OSS文件路径", required = true)
            @RequestParam("ossPath") String ossPath
    ) {
        log.info("========== 文件删除请求开始 ==========");
        log.info("接收到的ossPath参数：[{}]", ossPath);
        log.info("参数长度：{}", ossPath == null ? "null" : ossPath.length());

        ossService.deleteFile(ossPath);

        log.info("文件删除完成：{}", ossPath);
        log.info("========== 文件删除请求结束 ==========");
        
        return Result.success("文件删除成功", "删除成功");
    }

    /**
     * 获取临时访问URL
     */
    @GetMapping("/download-url")
    @Operation(summary = "获取临时访问URL", description = "生成OSS文件的临时访问链接")
    public Result<String> getDownloadUrl(
            @Parameter(description = "OSS文件路径", required = true)
            @RequestParam("ossPath") String ossPath,

            @Parameter(description = "过期时间（分钟）", required = false)
            @RequestParam(value = "expireMinutes", defaultValue = "60") Integer expireMinutes
    ) {
        log.info("生成临时URL，路径：{}，过期时间：{}分钟", ossPath, expireMinutes);

        String url = ossService.generateTemporaryUrl(ossPath, expireMinutes);

        return Result.success("URL生成成功", url);
    }


    /**
     * 列出指定目录下的文件
     */
    @GetMapping("/list")
    @Operation(summary = "列出文件", description = "列出指定目录下的所有文件")
    public Result<java.util.Map<String, Object>> listFiles(
            @Parameter(description = "文件夹路径", required = true)
            @RequestParam("folderPath") String folderPath
    ) {
        log.info("========== 列出文件请求 ==========");
        log.info("文件夹路径：{}", folderPath);

        java.util.Map<String, Object> result = ossService.listFiles(folderPath);

        log.info("========== 列出文件完成 ==========");
        log.info("文件数量：{}", result.get("fileCount"));

        return Result.success("获取成功", result);
    }

    /**
     * 根据员工姓名列出已签字文件
     */
    @GetMapping("/list-by-employee")
    @Operation(summary = "根据员工姓名列出已签字文件", description = "从effective-contracts目录中筛选该员工的所有已签字合同")
    public Result<java.util.Map<String, Object>> listFilesByEmployeeName(
            @Parameter(description = "员工姓名", required = true)
            @RequestParam("employeeName") String employeeName
    ) {
        log.info("========== 根据员工姓名列出文件 ==========");
        log.info("员工姓名：{}", employeeName);

        java.util.Map<String, Object> result = ossService.listFilesByEmployeeName(employeeName);

        log.info("========== 列出文件完成 ==========");
        log.info("文件数量：{}", result.get("fileCount"));

        return Result.success("获取成功", result);
    }
}

