package com.tailai.file.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置
 *
 * @author Tailai
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * 访问端点
     */
    private String endpoint;

    /**
     * 访问密钥ID
     */
    private String accessKeyId;

    /**
     * 访问密钥
     */
    private String accessKeySecret;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 创建OSS客户端（增强版 - 解决SSL握手问题）
     */
    @Bean
    public OSS ossClient() {
        try {
            log.info("开始初始化OSS客户端，endpoint: {}, bucket: {}", endpoint, bucketName);
            
            // 创建ClientConfiguration，设置连接参数
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            
            // 设置连接超时时间（单位：毫秒）
            conf.setConnectionTimeout(30000); // 30秒
            
            // 设置Socket超时时间（单位：毫秒）
            conf.setSocketTimeout(60000); // 60秒
            
            // 设置失败请求重试次数，默认3次
            conf.setMaxErrorRetry(5); // 增加到5次
            
            // 设置连接池最大连接数
            conf.setMaxConnections(200);
            
            // 设置请求超时时间（单位：毫秒）
            conf.setRequestTimeout(120000); // 120秒
            
            // 设置连接的最大空闲时间
            conf.setIdleConnectionTime(60000); // 60秒
            
            // 开启CNAME支持（如果使用自定义域名）
            // conf.setSupportCname(true);
            
            // 开启二级域名访问（可以提高访问速度和稳定性）
            // conf.setSLDEnabled(true);
            
            // 设置用户代理
            conf.setUserAgent("TailaiCloud/1.0");
            
            // 创建凭证提供者
            DefaultCredentialProvider credentialProvider = 
                new DefaultCredentialProvider(accessKeyId, accessKeySecret);
            
            // 使用OSSClientBuilder创建OSS客户端
            OSS ossClient = new OSSClientBuilder().build(
                endpoint, 
                credentialProvider, 
                conf
            );
            
            log.info("OSS客户端初始化成功");
            return ossClient;
            
        } catch (Exception e) {
            log.error("OSS客户端初始化失败", e);
            throw new RuntimeException("OSS客户端初始化失败: " + e.getMessage(), e);
        }
    }
}

