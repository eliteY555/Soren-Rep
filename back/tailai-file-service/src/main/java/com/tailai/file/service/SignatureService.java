package com.tailai.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.tailai.common.exception.BusinessException;
import com.tailai.file.config.OssConfig;
import com.tailai.file.dto.SignatureRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 电子签名服务
 *
 * @author Tailai
 */
@Slf4j
@Service
public class SignatureService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    /**
     * 添加签名到PDF
     */
    public Map<String, Object> addSignatureToPdf(SignatureRequest request) throws Exception {
        log.info("开始处理签名，PDF路径：{}", request.getPdfPath());

        // 1. 从OSS下载原PDF
        File originalPdf = downloadPdfFromOss(request.getPdfPath());
        log.info("原PDF下载完成：{}", originalPdf.getAbsolutePath());

        // 2. 将Base64签名转换为图片文件
        File signatureImage = base64ToImageFile(request.getSignatureBase64());
        log.info("签名图片生成完成：{}", signatureImage.getAbsolutePath());

        // 3. 计算签名哈希值
        String signatureHash = calculateHash(request.getSignatureBase64());
        log.info("签名哈希值：{}", signatureHash);

        // 4. 将签名叠加到PDF（使用默认位置和尺寸）
        File signedPdf = addSignatureImageToPdf(
                originalPdf,
                signatureImage,
                request.getSignerName(),
                request.getSignerRole()
        );
        log.info("签名已叠加到PDF：{}", signedPdf.getAbsolutePath());

        // 5. 上传签名后的PDF到OSS（员工签名保存到application目录，领导签名保存到effective-contracts目录）
        String signedPdfPath = uploadSignedPdfToOss(signedPdf, request.getPdfPath(), request.getEmployeeName(), request.getSignerRole());
        log.info("签名后PDF已上传到OSS：{}", signedPdfPath);

        // 6. 生成访问URL
        String signedPdfUrl = generateFileUrl(signedPdfPath);

        // 7. 清理临时文件
        cleanupTempFiles(originalPdf, signatureImage, signedPdf);

        // 8. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("signedPdfPath", signedPdfPath);
        result.put("signedPdfUrl", signedPdfUrl);
        result.put("signatureHash", signatureHash);
        result.put("signatureTime", System.currentTimeMillis());
        result.put("employeeName", request.getEmployeeName());
        result.put("signerName", request.getSignerName());
        result.put("signerRole", request.getSignerRole());

        log.info("签名处理完成");
        return result;
    }

    /**
     * 从OSS下载PDF
     */
    private File downloadPdfFromOss(String ossPath) throws IOException {
        log.info("从OSS下载PDF：{}，Bucket：{}", ossPath, ossConfig.getBucketName());

        // 检查文件是否存在
        boolean exists = ossClient.doesObjectExist(ossConfig.getBucketName(), ossPath);
        if (!exists) {
            log.error("OSS中PDF文件不存在，路径：{}，Bucket：{}", ossPath, ossConfig.getBucketName());
            throw new BusinessException("PDF文件不存在：" + ossPath);
        }
        
        log.info("PDF文件存在，开始下载...");

        // 下载文件
        OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), ossPath);
        InputStream inputStream = ossObject.getObjectContent();

        // 保存到临时文件
        File tempFile = File.createTempFile("original_", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } finally {
            inputStream.close();
        }

        log.info("PDF下载完成，临时文件：{}，大小：{} bytes", tempFile.getAbsolutePath(), tempFile.length());
        
        if (tempFile.length() == 0) {
            log.error("下载的PDF文件为空！");
            throw new IOException("下载的PDF文件为空");
        }
        
        return tempFile;
    }

    /**
     * Base64转图片文件
     */
    private File base64ToImageFile(String base64Data) throws IOException {
        log.info("转换Base64签名为图片，原始数据长度：{}", base64Data.length());

        // 去除可能的 Data URL 前缀（如：data:image/png;base64,）
        String cleanBase64 = base64Data;
        if (base64Data.contains(",")) {
            cleanBase64 = base64Data.substring(base64Data.indexOf(",") + 1);
            log.info("检测到Data URL前缀，已去除。纯Base64长度：{}", cleanBase64.length());
        }

        // 解码Base64
        byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

        // 保存到临时文件
        File tempFile = File.createTempFile("signature_", ".png");
        Files.write(tempFile.toPath(), imageBytes);

        log.info("签名图片生成完成，大小：{} bytes", tempFile.length());
        return tempFile;
    }

    /**
     * 计算签名哈希值（SHA-256）
     */
    private String calculateHash(String base64Data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(base64Data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * 将签名叠加到PDF（使用默认位置和尺寸）
     */
    private File addSignatureImageToPdf(
            File originalPdf,
            File signatureImage,
            String signerName,
            String signerRole
    ) throws IOException {
        log.info("开始叠加签名到PDF，原PDF大小：{} bytes，签名人角色：{}", originalPdf.length(), signerRole);

        PDDocument document = null;
        try {
            // 加载PDF文档
            document = PDDocument.load(originalPdf);
            int pageCount = document.getNumberOfPages();
            log.info("PDF加载成功，总页数：{}", pageCount);
            
            if (pageCount == 0) {
                log.error("PDF文档没有页面！");
                throw new IOException("PDF文档没有页面");
            }

            // 获取最后一页
            PDPage lastPage = document.getPage(pageCount - 1);
            PDRectangle pageSize = lastPage.getMediaBox();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            log.info("最后一页尺寸：{} x {}", pageWidth, pageHeight);

            // 签名尺寸
            int width = 120; // 签名宽度
            int height = 60; // 签名高度
            
            // 根据签名角色设置位置（新流程）
            float x, signatureY;
            if ("EMPLOYEE".equals(signerRole) || "员工".equals(signerRole)) {
                // 员工签名在右下角
                x = pageWidth - width - 50; // 距离右边50像素
                signatureY = 50; // 距离底部50像素
                log.info("员工签名位置：右下角 ({}, {})", x, signatureY);
            } else if ("MANAGER".equals(signerRole) || "经理".equals(signerRole) || "领导".equals(signerRole)) {
                // 经理/领导签名在左下角
                x = 50; // 距离左边50像素
                signatureY = 50; // 距离底部50像素
                log.info("经理签名位置：左下角 ({}, {})", x, signatureY);
            } else {
                // 默认位置（左下角）
                x = 50;
                signatureY = 50;
                log.warn("未识别的签名角色：{}，使用默认位置", signerRole);
            }

            // 创建内容流
            PDPageContentStream contentStream = new PDPageContentStream(
                    document,
                    lastPage,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            // 添加签名图片
            PDImageXObject pdImage = PDImageXObject.createFromFile(
                    signatureImage.getAbsolutePath(),
                    document
            );
            contentStream.drawImage(pdImage, x, signatureY, width, height);

            // 添加签名人信息文字（签名图片上方）
            String signatureInfo = signerName + " (" + signerRole + ")";
            String signatureTime = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );

            // 加载中文字体
            try {
                InputStream fontStream = getClass().getResourceAsStream("/fonts/SimSun.ttf");
                if (fontStream == null) {
                    log.warn("未找到中文字体文件，使用默认字体");
                    // 不添加中文文本，避免乱码
                } else {
                    PDType0Font font = PDType0Font.load(document, fontStream);
                    contentStream.setFont(font, 10);
                    
                    // 签名人信息显示在签名图片上方
                    contentStream.beginText();
                    contentStream.newLineAtOffset(x, signatureY + height + 5);
                    contentStream.showText(signatureInfo);
                    contentStream.endText();

                    // 签名时间显示在签名人信息上方
                    contentStream.beginText();
                    contentStream.newLineAtOffset(x, signatureY + height + 18);
                    contentStream.showText(signatureTime);
                    contentStream.endText();
                }
            } catch (Exception e) {
                log.warn("添加文字时出错，跳过文字添加：{}", e.getMessage());
            }

            contentStream.close();

            // 保存到新文件
            File signedPdf = File.createTempFile("signed_", ".pdf");
            document.save(signedPdf);
            log.info("签名叠加完成，保存到临时文件：{}，大小：{} bytes", 
                    signedPdf.getAbsolutePath(), signedPdf.length());
            
            if (signedPdf.length() == 0) {
                log.error("签名后的PDF文件为空！");
                throw new IOException("签名后的PDF文件为空");
            }

            return signedPdf;

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * 上传签名后的PDF到OSS（新流程）
     * 按照规则：
     * - 经理签名：直接更新原PDF文件（pending/contracts/）
     * - 员工签名：effective-contracts/员工姓名_劳动合同.pdf（合同生效）
     */
    private String uploadSignedPdfToOss(File signedPdf, String originalPath, String employeeName, String signerRole) throws IOException {
        String targetPath;
        
        if ("EMPLOYEE".equals(signerRole) || "员工".equals(signerRole)) {
            // 员工签名后，合同生效，保存到正式目录
            targetPath = "effective-contracts/" + employeeName + "_劳动合同.pdf";
            log.info("员工签名完成，合同生效，保存路径：{}", targetPath);
        } else if ("MANAGER".equals(signerRole) || "经理".equals(signerRole)) {
            // 经理签名后，直接更新原PDF文件（保持在pending/contracts/）
            targetPath = originalPath;
            log.info("经理签名完成，更新原PDF文件：{}", targetPath);
        } else {
            // 默认保存到正式目录
            targetPath = "effective-contracts/" + employeeName + "_劳动合同.pdf";
            log.info("其他签名角色，保存到正式目录：{}", targetPath);
        }
        
        log.info("上传签名后PDF - 原路径：{}，签名人角色：{}，目标路径：{}", originalPath, signerRole, targetPath);

        // 设置文件元信息，确保Content-Type正确
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/pdf");
        metadata.setContentLength(signedPdf.length());

        try (FileInputStream fis = new FileInputStream(signedPdf)) {
            ossClient.putObject(ossConfig.getBucketName(), targetPath, fis, metadata);
        }

        log.info("签名后PDF已上传到OSS：{}", targetPath);
        return targetPath;
    }

    /**
     * 生成文件访问URL
     */
    private String generateFileUrl(String ossPath) {
        String endpoint = ossConfig.getEndpoint().replace("https://", "").replace("http://", "");
        return "https://" + ossConfig.getBucketName() + "." + endpoint + "/" + ossPath;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                boolean deleted = file.delete();
                log.info("清理临时文件：{} - {}", file.getName(), deleted ? "成功" : "失败");
            }
        }
    }
}

