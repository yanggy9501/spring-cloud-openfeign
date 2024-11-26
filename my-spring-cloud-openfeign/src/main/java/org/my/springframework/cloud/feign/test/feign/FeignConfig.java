package org.my.springframework.cloud.feign.test.feign;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan("org.my.springframework.cloud.feign.test")
@Configuration
@EnableFeignClients
@EnableAutoConfiguration
public class FeignConfig {

}
