package org.my.springframework.cloud.feign;

//import feign.Contract;
//import feign.Feign;

/**
 * 为支持复杂对象类型查询参数自动配置类
 */
// @see https://www.jianshu.com/p/942c5c8f828a
//@Configuration
//@ConditionalOnClass(Feign.class)
//@ConditionalOnProperty(prefix = "feign.request", name = "object", havingValue = "true", matchIfMissing = true)
public class FeignRequestObjectAutoConfiguration {
	/**
	 * 覆盖FeignClientsConfiguration默认
	 */
//	@Bean
//	public Contract feignContract(ConversionService feignConversionService) {
//		List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();
//		annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
//		annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
//		annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());
//		// 新增的处理复杂对象类型查询参数
//		annotatedArgumentResolvers.add(new RequestObjectParameterProcessor());
//		return new SpringMvcContract(annotatedArgumentResolvers, feignConversionService);
//	}

	/**
	 * 覆盖FeignClientsConfiguration默认
	 */
//	@Configuration
//	@ConditionalOnClass({HystrixCommand.class, HystrixFeign.class})
//	protected static class HystrixFeignConfiguration {
//		@Bean
//		@Scope("prototype")
//		@ConditionalOnProperty(name = "feign.hystrix.enabled")
//		public Feign.Builder feignHystrixBuilder() {
//			HystrixFeign.Builder builder = HystrixFeign.builder();
//			builder.queryMapEncoder(new RequestObjectQueryMapEncoder());
//			return builder;
//		}
//	}
}
