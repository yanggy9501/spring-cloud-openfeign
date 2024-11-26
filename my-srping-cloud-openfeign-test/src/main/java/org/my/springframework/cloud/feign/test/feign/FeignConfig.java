package org.my.springframework.cloud.feign.test.feign;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

@ComponentScan("org.my.springframework.cloud.feign")
@Configuration
@EnableFeignClients
@EnableAutoConfiguration
@PropertySource(value = "classpath:application.properties")
public class FeignConfig {

}
