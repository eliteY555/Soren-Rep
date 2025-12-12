package com.tailai.miniapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 员工小程序服务启动类
 *
 * @author Tailai
 */
@SpringBootApplication
@MapperScan("com.tailai.miniapp.mapper")
@ComponentScan(basePackages = {"com.tailai.miniapp", "com.tailai.common"})
public class EmployeeMiniappApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeMiniappApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("员工小程序服务启动成功！");
        System.out.println("Knife4j文档地址: http://localhost:8081/doc.html");
        System.out.println("========================================\n");
    }
}


