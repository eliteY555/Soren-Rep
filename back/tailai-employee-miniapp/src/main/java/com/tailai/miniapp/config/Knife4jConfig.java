package com.tailai.miniapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置类
 *
 * @author Tailai
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("员工小程序API文档")
                        .version("1.0.0")
                        .description("泰来云 - 员工小程序服务接口")
                        .contact(new Contact()
                                .name("泰来云开发团队")
                                .email("dev@tailai.com")
                                .url("https://tailai.com")));
    }
}







