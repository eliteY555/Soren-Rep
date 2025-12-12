package com.tailai.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.tailai.common.exception.BusinessException;
import com.tailai.file.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 合同PDF生成服务
 *
 * @author Tailai
 */
@Slf4j
@Service
public class ContractPdfService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    /**
     * 从模板生成合同PDF（简化版：直接复制模板，不生成文字）
     * 
     * @param templateOssPath 模板OSS路径（直接传入完整路径，不再使用ID拼接）
     * @param contractData 合同数据（员工姓名、岗位、薪资等）
     * @param outputPath 输出路径（OSS路径）
     * @return 生成的PDF在OSS上的路径
     */
    public Map<String, Object> generateContractPdfFromTemplate(String templateOssPath, Map<String, Object> contractData, String outputPath) {
        log.info("开始生成合同PDF，模板路径：{}，输出路径：{}", templateOssPath, outputPath);
        log.info("合同数据：{}", contractData);
        
        File tempPdf = null;
        
        try {
            // 1. 从OSS下载模板PDF
            tempPdf = downloadTemplateFile(templateOssPath);
            
            log.info("模板PDF下载成功，文件大小：{} bytes", tempPdf.length());
            
            // 2. 直接上传模板副本到目标路径（不做任何修改）
            // 后续会通过签名服务在这个PDF上叠加签名
            String ossPath = uploadPdfToOss(tempPdf, outputPath);
            log.info("合同PDF已上传到OSS：{}", ossPath);
            
            // 3. 生成访问URL
            String pdfUrl = generateFileUrl(ossPath);
            
            // 4. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("pdfPath", ossPath);
            result.put("pdfUrl", pdfUrl);
            
            return result;
            
        } catch (Exception e) {
            log.error("生成合同PDF失败", e);
            throw new BusinessException("生成合同PDF失败：" + e.getMessage());
        } finally {
            // 清理资源
            try {
                if (tempPdf != null && tempPdf.exists()) {
                    tempPdf.delete();
                }
            } catch (Exception e) {
                log.warn("清理临时文件失败", e);
            }
        }
    }

    /**
     * 从OSS下载模板文件到本地临时文件
     */
    private File downloadTemplateFile(String templatePath) throws IOException {
        try {
            log.info("从OSS下载模板：{}", templatePath);
            
            // 检查模板是否存在
            if (!ossClient.doesObjectExist(ossConfig.getBucketName(), templatePath)) {
                throw new BusinessException("模板文件不存在：" + templatePath + "，请先在人事端上传合同模板");
            }
            
            InputStream inputStream = ossClient.getObject(ossConfig.getBucketName(), templatePath).getObjectContent();
            
            // 保存到临时文件
            File tempFile = File.createTempFile("template_", ".pdf");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            return tempFile;
        } catch (Exception e) {
            log.error("下载模板失败", e);
            throw new IOException("下载模板失败：" + e.getMessage(), e);
        }
    }

    /**
     * 上传PDF到OSS
     */
    private String uploadPdfToOss(File pdfFile, String outputPath) throws IOException {
        log.info("准备上传PDF到OSS，文件大小：{} bytes，目标路径：{}", pdfFile.length(), outputPath);
        
        if (pdfFile.length() == 0) {
            throw new IOException("PDF文件为空，无法上传");
        }
        
        try (InputStream inputStream = new FileInputStream(pdfFile)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/pdf");
            metadata.setContentLength(pdfFile.length());
            
            ossClient.putObject(ossConfig.getBucketName(), outputPath, inputStream, metadata);
            log.info("PDF已成功上传到OSS：{}，大小：{} bytes", outputPath, pdfFile.length());
            
            return outputPath;
        }
    }

    /**
     * 生成文件访问URL
     */
    private String generateFileUrl(String ossPath) {
        // OSS公共读bucket可以直接拼接URL
        // https://{bucket}.{endpoint}/{object}
        return String.format("https://%s.%s/%s", 
            ossConfig.getBucketName(), 
            ossConfig.getEndpoint().replace("https://", "").replace("http://", ""), 
            ossPath);
    }
}

