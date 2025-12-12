package com.tailai.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 合同签署请求DTO
 *
 * @author Tailai
 */
@Data
public class ContractSignRequest {

    /**
     * 合同ID
     */
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    /**
     * 签名Base64数据
     */
    @NotBlank(message = "签名数据不能为空")
    private String signatureBase64;

    /**
     * 签署IP地址
     */
    private String ipAddress;

    /**
     * 设备信息
     */
    private String deviceInfo;
}

