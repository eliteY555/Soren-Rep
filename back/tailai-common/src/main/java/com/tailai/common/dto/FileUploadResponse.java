package com.tailai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应
 *
 * @author Tailai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /**
     * OSS文件路径
     */
    private String ossPath;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 上传时间
     */
    private Long uploadTime;
}

