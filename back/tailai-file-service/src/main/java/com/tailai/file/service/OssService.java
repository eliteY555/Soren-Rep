package com.tailai.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.tailai.common.exception.BusinessException;
import com.tailai.file.config.OssConfig;
import com.tailai.file.dto.FileUploadResponse;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * OSS文件服务
 *
 * @author Tailai
 */
@Slf4j
@Service
public class OssService {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    /**
     * 上传文件到OSS（默认配置）
     *
     * @param file      文件
     * @param directory 目录（如：templates/contracts/）
     * @return 上传响应
     */
    public FileUploadResponse uploadFile(MultipartFile file, String directory) {
        return uploadFile(file, directory, true, true, null);
    }

    /**
     * 上传文件到OSS（完全自定义路径）
     *
     * @param file           文件
     * @param customPath     自定义完整路径（如：my/custom/path/myfile.pdf）
     * @param useCustomPath  是否使用自定义完整路径
     * @return 上传响应
     */
    public FileUploadResponse uploadFileWithCustomPath(MultipartFile file, String customPath, boolean useCustomPath) {
        if (useCustomPath) {
            return uploadFile(file, null, false, false, customPath);
        } else {
            return uploadFile(file, customPath, true, true, null);
        }
    }

    /**
     * 上传文件到OSS（完整配置）
     *
     * @param file              文件
     * @param directory         目录前缀（如：templates/contracts/）
     * @param useDateSubDir     是否使用日期子目录（yyyy/MM/dd）
     * @param useUuidFileName   是否使用UUID文件名
     * @param fullCustomPath    完整自定义路径（优先级最高，如果提供则忽略其他参数）
     * @return 上传响应
     */
    public FileUploadResponse uploadFile(MultipartFile file, String directory, 
                                         boolean useDateSubDir, boolean useUuidFileName, 
                                         String fullCustomPath) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("文件名不能为空");
        }

        try {
            String ossPath;
            
            // 如果提供了完整自定义路径，直接使用
            if (fullCustomPath != null && !fullCustomPath.trim().isEmpty()) {
                ossPath = fullCustomPath.trim();
                log.info("使用完全自定义路径：{}", ossPath);
            } else {
                // 获取文件扩展名
                String extension = getFileExtension(originalFilename);
                
                // 确定文件名
                String fileName;
                if (useUuidFileName) {
                    fileName = generateFileName(extension);
                } else {
                    fileName = originalFilename;
                }
                
                // 构建OSS路径
                ossPath = buildOssPath(directory, fileName, useDateSubDir);
            }

            // 设置文件元信息
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            
            // 根据文件扩展名设置Content-Type，确保PDF文件类型正确
            String contentType = file.getContentType();
            if (ossPath.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            }
            metadata.setContentType(contentType);
            
            // 设置Content-Disposition为inline，让浏览器预览而不是下载
            // 对于PDF、图片等可预览的文件类型使用inline
            if (isPreviewableFile(ossPath)) {
                metadata.setContentDisposition("inline");
                log.info("文件设置为预览模式（inline）：{}", ossPath);
            }

            // 上传到OSS（带重试机制）
            InputStream inputStream = file.getInputStream();
            uploadWithRetry(ossPath, inputStream, metadata);

            log.info("文件上传成功：{}", ossPath);

            // 构建响应（不再返回直接访问URL，需要时通过download-url接口获取临时URL）
            return new FileUploadResponse(
                    ossPath,
                    null, // 不再返回直接URL，统一使用临时访问URL
                    originalFilename,
                    file.getSize(),
                    file.getContentType(),
                    System.currentTimeMillis()
            );

        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除OSS文件
     *
     * @param ossPath OSS文件路径
     */
    public void deleteFile(String ossPath) {
        if (ossPath == null || ossPath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }
        
        try {
            log.info("准备删除OSS文件，Bucket: {}, 路径: {}", ossConfig.getBucketName(), ossPath);
            
            // 检查文件是否存在
            boolean exists = ossClient.doesObjectExist(ossConfig.getBucketName(), ossPath);
            if (!exists) {
                log.warn("文件不存在，无需删除：{}", ossPath);
                throw new BusinessException("文件不存在：" + ossPath);
            }
            
            // 删除文件
            ossClient.deleteObject(ossConfig.getBucketName(), ossPath);
            
            // 再次检查是否删除成功
            boolean stillExists = ossClient.doesObjectExist(ossConfig.getBucketName(), ossPath);
            if (stillExists) {
                log.error("文件删除失败，文件仍然存在：{}", ossPath);
                throw new BusinessException("文件删除失败，文件仍然存在");
            }
            
            log.info("文件删除成功：{}", ossPath);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除OSS文件时发生异常，Bucket: {}, 路径: {}, 错误: {}", 
                    ossConfig.getBucketName(), ossPath, e.getMessage(), e);
            throw new BusinessException("文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 生成临时访问URL
     *
     * @param ossPath       OSS文件路径
     * @param expireMinutes 过期时间（分钟）
     * @return 临时访问URL
     */
    public String generateTemporaryUrl(String ossPath, Integer expireMinutes) {
        if (ossPath == null || ossPath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }
        
        try {
            log.info("准备生成临时URL，Bucket: {}, 路径: {}", ossConfig.getBucketName(), ossPath);
            
            // 检查文件是否存在
            boolean exists = ossClient.doesObjectExist(ossConfig.getBucketName(), ossPath);
            if (!exists) {
                log.warn("文件不存在，无法生成URL：{}", ossPath);
                throw new BusinessException("文件不存在：" + ossPath);
            }
            
            // 设置过期时间
            Date expiration = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000);

            // 创建请求对象
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossConfig.getBucketName(),
                    ossPath
            );
            request.setExpiration(expiration);
            
            // 设置响应头，让浏览器预览而不是下载
            // 注意：阿里云OSS不允许覆盖Content-Type，必须在上传时设置正确
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            
            // 对于可预览的文件（PDF、图片），设置为inline
            if (isPreviewableFile(ossPath)) {
                responseHeaders.setContentDisposition("inline");
                log.info("设置Content-Disposition为inline（预览模式）");
            } else {
                // 其他文件设置为下载
                responseHeaders.setContentDisposition("attachment");
                log.info("设置Content-Disposition为attachment（下载模式）");
            }
            
            request.setResponseHeaders(responseHeaders);

            // 生成签名URL
            URL url = ossClient.generatePresignedUrl(request);

            log.info("临时URL生成成功，过期时间：{}，文件：{}", expiration, ossPath);
            return url.toString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成临时URL失败：{}", e.getMessage(), e);
            throw new BusinessException("生成临时URL失败：" + e.getMessage());
        }
    }


    /**
     * 判断文件是否可预览
     */
    private boolean isPreviewableFile(String filePath) {
        String lowerPath = filePath.toLowerCase();
        // PDF和图片文件可以预览
        return lowerPath.endsWith(".pdf") 
            || lowerPath.endsWith(".png") 
            || lowerPath.endsWith(".jpg") 
            || lowerPath.endsWith(".jpeg") 
            || lowerPath.endsWith(".gif") 
            || lowerPath.endsWith(".bmp")
            || lowerPath.endsWith(".webp");
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String extension) {
        return IdUtil.simpleUUID() + extension;
    }

    /**
     * 构建OSS路径
     *
     * @param directory     目录前缀
     * @param fileName      文件名
     * @param useDateSubDir 是否使用日期子目录
     * @return OSS完整路径
     */
    private String buildOssPath(String directory, String fileName, boolean useDateSubDir) {
        StringBuilder pathBuilder = new StringBuilder();

        // 添加目录前缀
        if (directory != null && !directory.isEmpty()) {
            pathBuilder.append(directory);
            // 确保目录以/结尾
            if (!directory.endsWith("/")) {
                pathBuilder.append("/");
            }
        }

        // 添加日期子目录（可选）
        if (useDateSubDir) {
            String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
            pathBuilder.append(datePath).append("/");
        }

        // 添加文件名
        pathBuilder.append(fileName);

        return pathBuilder.toString();
    }

    /**
     * 列出指定目录下的文件
     *
     * @param folderPath 文件夹路径（如：张三/）
     * @return 文件列表信息
     */
    public Map<String, Object> listFiles(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            throw new BusinessException("文件夹路径不能为空");
        }
        
        try {
            log.info("列出文件，Bucket: {}, 文件夹: {}", ossConfig.getBucketName(), folderPath);
            
            // 列出指定前缀的所有对象
            ObjectListing objectListing = ossClient.listObjects(ossConfig.getBucketName(), folderPath);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            
            // 构建文件列表
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (OSSObjectSummary summary : objectSummaries) {
                // 跳过目录本身
                if (summary.getKey().equals(folderPath)) {
                    continue;
                }
                
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("fileName", extractFileName(summary.getKey()));
                fileInfo.put("filePath", summary.getKey());
                fileInfo.put("fileSize", summary.getSize());
                fileInfo.put("lastModified", summary.getLastModified().getTime());
                
                fileList.add(fileInfo);
            }
            
            log.info("找到 {} 个文件", fileList.size());
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("folderPath", folderPath);
            result.put("fileCount", fileList.size());
            result.put("files", fileList);
            
            return result;
            
        } catch (Exception e) {
            log.error("列出文件失败：{}", e.getMessage(), e);
            throw new BusinessException("列出文件失败：" + e.getMessage());
        }
    }

    /**
     * 根据员工姓名列出已签字文件
     * 从effective-contracts目录中筛选该员工的文件
     *
     * @param employeeName 员工姓名
     * @return 文件列表信息
     */
    public Map<String, Object> listFilesByEmployeeName(String employeeName) {
        if (employeeName == null || employeeName.trim().isEmpty()) {
            throw new BusinessException("员工姓名不能为空");
        }
        
        try {
            log.info("根据员工姓名列出文件，Bucket: {}, 员工: {}", ossConfig.getBucketName(), employeeName);
            
            // 列出effective-contracts目录下的所有文件
            String folderPath = "effective-contracts/";
            ObjectListing objectListing = ossClient.listObjects(ossConfig.getBucketName(), folderPath);
            List<OSSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            
            // 筛选该员工的文件（文件名以"员工姓名_"开头）
            String filePrefix = employeeName + "_";
            List<Map<String, Object>> fileList = new ArrayList<>();
            
            for (OSSObjectSummary summary : objectSummaries) {
                String fileName = extractFileName(summary.getKey());
                
                // 跳过目录本身
                if (summary.getKey().equals(folderPath)) {
                    continue;
                }
                
                // 筛选该员工的文件
                if (fileName.startsWith(filePrefix)) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", fileName);
                    fileInfo.put("filePath", summary.getKey());
                    fileInfo.put("fileSize", summary.getSize());
                    fileInfo.put("lastModified", summary.getLastModified().getTime());
                    
                    fileList.add(fileInfo);
                }
            }
            
            log.info("找到员工 {} 的文件 {} 个", employeeName, fileList.size());
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("employeeName", employeeName);
            result.put("fileCount", fileList.size());
            result.put("files", fileList);
            
            return result;
            
        } catch (Exception e) {
            log.error("根据员工姓名列出文件失败：{}", e.getMessage(), e);
            throw new BusinessException("列出文件失败：" + e.getMessage());
        }
    }


    /**
     * 从完整路径中提取文件名
     */
    private String extractFileName(String fullPath) {
        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < fullPath.length() - 1) {
            return fullPath.substring(lastSlash + 1);
        }
        return fullPath;
    }

    /**
     * 带重试机制的文件上传
     * 解决SSL握手失败等网络问题
     *
     * @param ossPath    OSS路径
     * @param inputStream 文件输入流
     * @param metadata   文件元数据
     */
    private void uploadWithRetry(String ossPath, InputStream inputStream, ObjectMetadata metadata) {
        int maxRetries = 3; // 最大重试次数
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                // 如果是重试，需要重新标记流的位置（如果支持的话）
                if (retryCount > 0) {
                    if (inputStream.markSupported()) {
                        inputStream.reset();
                    }
                    log.info("正在进行第 {} 次重试上传...", retryCount);
                    // 重试前等待一段时间，避免立即重试
                    Thread.sleep(1000 * retryCount); // 递增等待时间
                }
                
                // 标记流位置（如果支持）
                if (inputStream.markSupported()) {
                    inputStream.mark(Integer.MAX_VALUE);
                }
                
                // 执行上传
                ossClient.putObject(
                        ossConfig.getBucketName(),
                        ossPath,
                        inputStream,
                        metadata
                );
                
                // 上传成功，跳出循环
                if (retryCount > 0) {
                    log.info("重试上传成功！");
                }
                return;
                
            } catch (com.aliyun.oss.ClientException e) {
                lastException = e;
                retryCount++;
                
                // 判断是否是SSL相关异常
                boolean isSslError = e.getMessage() != null && 
                    (e.getMessage().contains("SSL") || 
                     e.getMessage().contains("handshake") ||
                     e.getMessage().contains("SslException"));
                
                if (isSslError) {
                    log.warn("检测到SSL握手异常，第 {} 次尝试失败：{}", retryCount, e.getMessage());
                } else {
                    log.warn("上传失败，第 {} 次尝试失败：{}", retryCount, e.getMessage());
                }
                
                // 如果已达到最大重试次数，抛出异常
                if (retryCount >= maxRetries) {
                    log.error("文件上传失败，已达到最大重试次数 {}", maxRetries);
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("上传过程被中断：" + e.getMessage());
            } catch (IOException e) {
                lastException = e;
                log.error("读取文件流失败：{}", e.getMessage(), e);
                throw new BusinessException("读取文件流失败：" + e.getMessage());
            }
        }
        
        // 所有重试都失败，抛出异常
        if (lastException != null) {
            log.error("文件上传最终失败，已重试 {} 次", retryCount, lastException);
            throw new BusinessException("文件上传失败（已重试" + retryCount + "次）：" + lastException.getMessage());
        }
    }

}

