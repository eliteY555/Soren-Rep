package com.tailai.common.enums;

/**
 * 签署人角色枚举
 */
public enum SignerRole {
    /**
     * 经理
     */
    MANAGER("MANAGER", "经理"),
    
    /**
     * 员工
     */
    EMPLOYEE("EMPLOYEE", "员工");

    private final String code;
    private final String desc;

    SignerRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SignerRole fromCode(String code) {
        for (SignerRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown signer role code: " + code);
    }
}

