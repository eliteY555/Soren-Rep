package com.tailai.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "文件上传响应")
public class FileUploadResponse {

    @Schema(description = "OSS文件路径")
    private String ossPath;

    @Schema(description = "文件访问URL")
    private String fileUrl;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "上传时间")
    private Long uploadTime;
}

