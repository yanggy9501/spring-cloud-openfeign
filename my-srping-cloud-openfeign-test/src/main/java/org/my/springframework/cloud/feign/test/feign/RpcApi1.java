package org.my.springframework.cloud.feign.test.feign;

import org.my.springframework.cloud.feign.test.vo.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "RpcApi1", url = "127.0.0.1:8080/hy")
public interface RpcApi1 {
	@GetMapping("/ok")
	String f1();

	@GetMapping("/u")
	User fu(User user);
}
