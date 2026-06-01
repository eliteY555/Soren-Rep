package com.me.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类 — 使用 BCrypt 单向哈希替代 AES 可逆加密
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 对原始密码进行 BCrypt 哈希（注册/修改密码时调用）
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证原始密码与哈希值是否匹配（登录时调用）
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 命令行入口：生成 BCrypt 哈希（用于种子数据迁移）
     * 用法：java -cp ... com.me.utils.PasswordUtil <rawPassword>
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("用法: java com.me.utils.PasswordUtil <原始密码>");
            System.out.println("示例: java com.me.utils.PasswordUtil 123456");
            return;
        }
        System.out.println("原始密码: " + args[0]);
        System.out.println("BCrypt哈希: " + encode(args[0]));
    }
}
