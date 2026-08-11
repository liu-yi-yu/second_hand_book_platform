package org.tlais.yutest1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.tlais.yutest1.properties.JwtProperties;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableConfigurationProperties(JwtProperties.class)
@EnableCaching//开启缓存功能
@EnableScheduling//开启定时任务功能
public class YuTest1Application {

	public static void main(String[] args) {
		SpringApplication.run(YuTest1Application.class, args);
	}

}
