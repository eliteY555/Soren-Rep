package com.tailai.common.enums;

/**
 * 合同类型枚举
 */
public enum ContractType {
    /**
     * 入职合同
     */
    ONBOARD("ONBOARD", "入职合同"),
    
    /**
     * 离职协议
     */
    OFFBOARD("OFFBOARD", "离职协议");

    private final String code;
    private final String desc;

    ContractType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ContractType fromCode(String code) {
        for (ContractType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown contract type code: " + code);
    }
}

