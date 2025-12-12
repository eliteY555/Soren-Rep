package com.tailai.file;

import com.tailai.common.client.FileServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 文件服务启动类
 *
 * @author Tailai
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@ComponentScan(
    basePackages = {"com.tailai.file", "com.tailai.common"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = FileServiceClient.class
    )
)
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("文件服务启动成功！");
        System.out.println("Knife4j文档地址: http://localhost:8090/doc.html");
        System.out.println("========================================\n");
    }
}

