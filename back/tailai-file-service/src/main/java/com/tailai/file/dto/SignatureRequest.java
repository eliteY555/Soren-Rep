package com.tailai.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签名请求DTO
 *
 * @author Tailai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "签名请求")
public class SignatureRequest {

    @Schema(description = "PDF文件OSS路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pdfPath;

    @Schema(description = "签名图片Base64编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signatureBase64;

    @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String employeeName;

    @Schema(description = "签名人姓名（领导）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signerName;

    @Schema(description = "签名人角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "LEADER")
    private String signerRole;

    @Schema(description = "合同ID（可选）")
    private Long contractId;
}

