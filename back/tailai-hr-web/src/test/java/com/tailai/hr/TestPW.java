package com.tailai.hr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class TestPW {

    @Autowired
    private Environment environment;

    @Test
    void testEnvironmentVariable() {
        // 直接从Environment获取属性
        String password = environment.getProperty("MYSQL_PW");

        System.out.println("MYSQL_PW 值: " + (password != null ? password : "未设置"));

        // 验证
        assertNotNull(password, "MYSQL_PW 环境变量未设置");

        // 也可以检查系统环境变量
        String sysEnv = System.getenv("MYSQL_PW");
        System.out.println("系统环境变量值: " + sysEnv);
    }
}