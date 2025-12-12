package com.tailai.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class TestPW {

    @Value("${spring.datasource.password:NOT_SET}")
    private String datasourcePassword;

    @Autowired
    private Environment env;

    @Test
    public void diagnoseEnvironment() {
        System.out.println("=== 环境变量诊断 ===");

        // 1. 检查系统环境变量
        String systemEnv = System.getenv("MYSQL_PW");
        System.out.println("System.getenv('MYSQL_PW'): " +
                (systemEnv != null ? "***" + systemEnv.substring(Math.max(0, systemEnv.length() - 2)) : "NULL"));

        // 2. 检查Spring属性
        System.out.println("@Value注入的密码: " +
                (datasourcePassword.equals("NOT_SET") ? "NOT_SET" : "***SET***"));

        // 3. 检查所有相关属性
        String[] props = {
                "spring.datasource.password",
                "MYSQL_PW"
        };

        for (String prop : props) {
            String value = env.getProperty(prop);
            System.out.println("Environment.getProperty('" + prop + "'): " +
                    (value != null ? "***SET***" : "NULL"));
        }

        // 4. 检查数据源URL和用户名
        System.out.println("数据源URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("数据源用户名: " + env.getProperty("spring.datasource.username"));
    }
}