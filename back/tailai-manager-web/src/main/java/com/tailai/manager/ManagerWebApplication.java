package com.tailai.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 经理端Web服务启动类
 *
 * @author Tailai
 */
@SpringBootApplication(scanBasePackages = {"com.tailai.manager", "com.tailai.common"})
@MapperScan("com.tailai.manager.mapper")
public class ManagerWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerWebApplication.class, args);
        System.out.println("========================================");
        System.out.println("经理端Web服务启动成功！");
        System.out.println("Knife4j文档地址: http://localhost:8082/doc.html");
        System.out.println("========================================");
    }
}

