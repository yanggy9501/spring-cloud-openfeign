package org.my.springframework.cloud.feign.test;

import org.my.springframework.cloud.feign.test.feign.FeignConfig;
import org.my.springframework.cloud.feign.test.feign.RpcApi1;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApiMain {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(FeignConfig.class);

		RpcApi1 bean = applicationContext.getBean(RpcApi1.class);
		System.out.println(bean);
		bean.f1();
	}
}
