package com.tailai.hr.config;

import io.swagger.v3.oas.models.OpenAPI;
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
                        .title("泰来云-人事端API文档")
                        .version("2.0.0")
                        .description("保安公司员工管理系统-人事端服务接口"));
    }
}

