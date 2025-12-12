package com.tailai.hr.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailai.common.client.FileServiceClient;
import com.tailai.common.dto.FileUploadResponse;
import com.tailai.common.entity.ContractTemplate;
import com.tailai.common.exception.BusinessException;
import com.tailai.common.result.Result;
import com.tailai.hr.mapper.ContractTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 模板管理服务
 *
 * @author Tailai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final ContractTemplateMapper templateMapper;
    private final FileServiceClient fileServiceClient;

    /**
     * 上传合同模板（上传文件并创建/更新记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> uploadTemplate(MultipartFile file, String templateName, String templateCode, 
                                        String templateType, String contractCategory, String remark, Long createdBy) {
        // 验证文件不为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        
        // 验证文件类型：只允许PDF
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        log.info("上传合同模板文件：{}，ContentType：{}", originalFilename, contentType);
        
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("只支持上传PDF文件，请选择.pdf格式的文件");
        }
        
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new BusinessException("文件类型错误，只支持PDF格式");
        }
        
        // 验证文件大小（限制20MB）
        long maxSize = 20 * 1024 * 1024; // 20MB
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过20MB");
        }

        // 检查模板编码是否已存在
        ContractTemplate exist = templateMapper.selectOne(
                new LambdaQueryWrapper<ContractTemplate>()
                        .eq(ContractTemplate::getTemplateCode, templateCode)
        );
        
        // 如果是更新操作且存在旧文件，先删除旧文件
        if (exist != null && exist.getOssFilePath() != null && !exist.getOssFilePath().trim().isEmpty()) {
            try {
                log.info("检测到旧模板文件，开始删除，路径：{}", exist.getOssFilePath());
                Result<String> deleteResult = fileServiceClient.deleteFile(exist.getOssFilePath());
                
                if (deleteResult.getCode() == 200) {
                    log.info("旧模板文件删除成功");
                } else {
                    log.warn("旧模板文件删除失败：{}，继续上传新文件", deleteResult.getMessage());
                }
            } catch (Exception e) {
                log.warn("删除旧模板文件时发生异常：{}，继续上传新文件", e.getMessage());
                // 删除失败不影响上传新文件
            }
        }
        
        // 调用文件服务上传新模板文件
        Result<FileUploadResponse> uploadResult = fileServiceClient.uploadTemplate(file);
        if (uploadResult.getCode() != 200 || uploadResult.getData() == null) {
            throw new BusinessException("模板文件上传失败：" + uploadResult.getMessage());
        }
        
        String ossFilePath = uploadResult.getData().getOssPath();
        log.info("新模板文件上传成功，OSS路径：{}", ossFilePath);
        
        if (exist != null) {
            // 模板已存在，更新文件路径和其他信息
            log.info("模板编码已存在，更新模板信息，模板ID：{}", exist.getId());
            exist.setTemplateName(templateName);
            exist.setTemplateType(templateType);
            exist.setContractCategory(contractCategory);
            exist.setOssFilePath(ossFilePath);
            exist.setRemark(remark);
            // 版本号自增
            String currentVersion = exist.getVersion();
            if (currentVersion != null && currentVersion.matches("\\d+\\.\\d+")) {
                String[] parts = currentVersion.split("\\.");
                int minorVersion = Integer.parseInt(parts[1]) + 1;
                exist.setVersion(parts[0] + "." + minorVersion);
            }
            templateMapper.updateById(exist);
            return Result.success(exist.getId());
        } else {
            // 创建新模板记录
            log.info("创建新模板记录");
            ContractTemplate template = new ContractTemplate();
            template.setTemplateName(templateName);
            template.setTemplateCode(templateCode);
            template.setTemplateType(templateType);
            template.setContractCategory(contractCategory);
            template.setOssFilePath(ossFilePath);
            template.setRemark(remark);
            template.setCreatedBy(createdBy);
            template.setStatus(1); // 默认启用
            template.setVersion("1.0");
            templateMapper.insert(template);
            return Result.success(template.getId());
        }
    }
    
    /**
     * 查询模板列表
     */
    public Result<Page<ContractTemplate>> getTemplateList(int pageNum, int pageSize, String templateType) {
        Page<ContractTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        
        if (templateType != null && !templateType.isEmpty()) {
            wrapper.eq(ContractTemplate::getTemplateType, templateType);
        }
        
        wrapper.orderByDesc(ContractTemplate::getCreatedAt);
        
        Page<ContractTemplate> result = templateMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取模板详情
     */
    public Result<ContractTemplate> getTemplateDetail(Long id) {
        ContractTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return Result.success(template);
    }

    /**
     * 删除模板（同时删除数据库记录和OSS文件）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteTemplate(Long id) {
        log.info("删除模板，ID：{}", id);
        
        ContractTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // TODO: 检查是否有合同在使用该模板
        
        // 删除数据库记录
        templateMapper.deleteById(id);
        log.info("数据库模板记录删除成功，ID：{}", id);
        
        // 删除OSS中的文件
        if (template.getOssFilePath() != null && !template.getOssFilePath().trim().isEmpty()) {
            try {
                log.info("开始删除OSS文件，路径：{}", template.getOssFilePath());
                Result<String> deleteResult = fileServiceClient.deleteFile(template.getOssFilePath());
                
                if (deleteResult.getCode() == 200) {
                    log.info("OSS文件删除成功，路径：{}", template.getOssFilePath());
                } else {
                    log.warn("OSS文件删除失败，路径：{}，原因：{}", template.getOssFilePath(), deleteResult.getMessage());
                    // 注意：即使OSS文件删除失败，数据库记录已删除，不影响主流程
                }
            } catch (Exception e) {
                log.error("删除OSS文件时发生异常，路径：{}", template.getOssFilePath(), e);
                // 异常不影响主流程，已经删除数据库记录
            }
        } else {
            log.info("模板无关联文件，跳过OSS文件删除");
        }

        return Result.success();
    }
    
    /**
     * 获取模板文件预览URL（已废弃，使用proxyFile代理下载）
     */
    @Deprecated
    public Result<String> getPreviewUrl(String ossPath, Integer expireMinutes) {
        log.info("获取模板文件预览URL，OSS路径：{}，过期时间：{}分钟", ossPath, expireMinutes);
        
        if (ossPath == null || ossPath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }
        
        // 调用文件服务获取临时访问URL
        Result<String> result = fileServiceClient.getDownloadUrl(ossPath, expireMinutes);
        
        if (result.getCode() != 200) {
            log.error("获取预览URL失败：{}", result.getMessage());
            throw new BusinessException("获取预览URL失败：" + result.getMessage());
        }
        
        log.info("预览URL生成成功");
        return result;
    }
    
    /**
     * 代理下载模板文件（解决CORS问题）
     */
    public void proxyFile(String ossPath, HttpServletResponse response) {
        log.info("代理下载文件，OSS路径：{}", ossPath);
        
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
            
            log.info("文件代理下载成功");
            
        } catch (Exception e) {
            log.error("文件代理下载失败：{}", e.getMessage(), e);
            throw new BusinessException("文件代理下载失败：" + e.getMessage());
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

