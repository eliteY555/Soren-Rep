package com.tailai.hr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 人事端Web服务启动类
 *
 * @author Tailai
 */
@SpringBootApplication(scanBasePackages = {"com.tailai.hr", "com.tailai.common"})
@MapperScan("com.tailai.hr.mapper")
public class HrWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrWebApplication.class, args);
        System.out.println("========================================");
        System.out.println("人事端Web服务启动成功！");
        System.out.println("Knife4j文档地址: http://localhost:8085/doc.html");
        System.out.println("========================================");
    }
}

