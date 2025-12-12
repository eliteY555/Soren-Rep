package com.tailai.common.enums;

/**
 * 合同状态枚举（简化流程）
 * 经理发起合同时直接签字，所以只需要三种状态
 */
public enum ContractStatus {
    /**
     * 待员工签字（经理已签，等待员工签字）
     */
    PENDING_EMPLOYEE_SIGN(1, "待员工签字"),
    
    /**
     * 已生效（员工已签字，合同生效）
     */
    EFFECTIVE(2, "已生效"),
    
    /**
     * 已终止
     */
    TERMINATED(3, "已终止");

    private final Integer code;
    private final String desc;

    ContractStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ContractStatus fromCode(Integer code) {
        for (ContractStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown contract status code: " + code);
    }
}

