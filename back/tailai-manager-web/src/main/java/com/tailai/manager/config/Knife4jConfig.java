package com.tailai.manager.config;

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
                        .title("泰来云-经理端API文档")
                        .version("2.0.0")
                        .description("保安公司员工管理系统-经理端服务接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@tailai.com")));
    }
}

