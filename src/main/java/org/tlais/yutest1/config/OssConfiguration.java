package org.tlais.yutest1.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tlais.yutest1.properties.AliOSSProperties;
import org.tlais.yutest1.util.AliOssUtil;

@Configuration
@Slf4j
public class OssConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOSSProperties aliyunOSSProperties) {
        log.info("开始创建阿里云文件上传客户端{}",aliyunOSSProperties);
        return new AliOssUtil(aliyunOSSProperties);
    }
}