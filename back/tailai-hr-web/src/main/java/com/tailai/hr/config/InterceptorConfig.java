package com.tailai.hr.config;

import com.tailai.hr.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器配置
 *
 * @author Tailai
 */
@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/hr/**")
                .excludePathPatterns(
                        "/api/hr/auth/login",
                        "/api/hr/auth/register",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v3/**"
                );
    }
}

