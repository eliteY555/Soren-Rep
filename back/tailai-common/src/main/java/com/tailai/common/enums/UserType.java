package com.tailai.common.enums;

/**
 * 用户类型枚举
 */
public enum UserType {
    /**
     * 员工
     */
    EMPLOYEE("EMPLOYEE", "员工"),
    
    /**
     * 经理
     */
    MANAGER("MANAGER", "经理"),
    
    /**
     * 人事
     */
    HR("HR", "人事");

    private final String code;
    private final String desc;

    UserType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static UserType fromCode(String code) {
        for (UserType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user type code: " + code);
    }
}

