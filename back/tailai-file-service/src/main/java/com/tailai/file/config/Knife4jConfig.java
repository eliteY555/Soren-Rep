package com.tailai.file.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置
 *
 * @author Tailai
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("文件服务API文档")
                        .version("1.0.0")
                        .description("泰来云 - 文件服务接口（OSS上传下载、PDF生成）")
                        .contact(new Contact()
                                .name("泰来云开发团队")
                                .url("https://tailai.com")
                                .email("dev@tailai.com")));
    }
}

