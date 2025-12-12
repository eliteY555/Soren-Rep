package com.tailai.common.constant;

/**
 * 合同相关常量
 *
 * @author Tailai
 */
public class ContractConstant {

    /**
     * 合同编号前缀
     */
    public static final String CONTRACT_NO_PREFIX_ONBOARD = "ON-";
    public static final String CONTRACT_NO_PREFIX_OFFBOARD = "OFF-";

    /**
     * 签名位置（PDF坐标）
     */
    public static final class SignaturePosition {
        /**
         * 经理签名位置（左下角）
         */
        public static final int MANAGER_X = 50;
        public static final int MANAGER_Y = 50;

        /**
         * 员工签名位置（右下角）
         */
        public static final int EMPLOYEE_X = 400;
        public static final int EMPLOYEE_Y = 50;

        /**
         * 签名尺寸
         */
        public static final int SIGNATURE_WIDTH = 120;
        public static final int SIGNATURE_HEIGHT = 60;
    }

    /**
     * OSS目录路径
     */
    public static final class OssPath {
        public static final String TEMPLATES = "templates/contracts/";
        public static final String TEMP_CONTRACTS = "temp-contracts/";
        public static final String EFFECTIVE_CONTRACTS = "effective-contracts/";
        public static final String CERTIFICATES = "certificates/";
        public static final String RETURN_ITEMS = "return-items/";
    }

    /**
     * 合同文件命名格式
     */
    public static final String CONTRACT_FILE_NAME_FORMAT = "%s_%s.pdf"; // 员工姓名_合同类型.pdf
}

