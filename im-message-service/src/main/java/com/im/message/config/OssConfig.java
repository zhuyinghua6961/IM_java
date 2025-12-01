package com.im.message.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 客户端配置
 */
@Configuration
public class OssConfig {

    @Value("${im.oss.endpoint}")
    private String endpoint;

    @Value("${im.oss.access-key-id}")
    private String accessKeyId;

    @Value("${im.oss.access-key-secret}")
    private String accessKeySecret;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
