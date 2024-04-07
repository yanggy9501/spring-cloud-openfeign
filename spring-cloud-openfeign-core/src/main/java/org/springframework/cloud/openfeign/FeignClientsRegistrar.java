/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * ImportBeanDefinitionRegistrar：用于向容器中注入bean 关注其 registerBeanDefinitions 方法
 *
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Venil Noronha
 * @author Gang Li
 */
// feign 客户端注册器，注册扫描 feign 注解的 BeanPostProcessor
class FeignClientsRegistrar
		implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	// patterned after Spring Integration IntegrationComponentScanRegistrar
	// and RibbonClientsConfigurationRegistgrar

	private ResourceLoader resourceLoader;

	private Environment environment;

	FeignClientsRegistrar() {
	}

	static void validateFallback(final Class clazz) {
		Assert.isTrue(!clazz.isInterface(),
				"Fallback class must implement the interface annotated by @FeignClient");
	}

	static void validateFallbackFactory(final Class clazz) {
		Assert.isTrue(!clazz.isInterface(), "Fallback factory must produce instances "
				+ "of fallback classes that implement the interface annotated by @FeignClient");
	}

	static String getName(String name) {
		if (!StringUtils.hasText(name)) {
			return "";
		}

		String host = null;
		try {
			String url;
			if (!name.startsWith("http://") && !name.startsWith("https://")) {
				url = "http://" + name;
			}
			else {
				url = name;
			}
			host = new URI(url).getHost();

		}
		catch (URISyntaxException e) {
		}
		Assert.state(host != null, "Service id not legal hostname (" + name + ")");
		return name;
	}

	static String getUrl(String url) {
		if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
			if (!url.contains("://")) {
				url = "http://" + url;
			}
			try {
				new URL(url);
			}
			catch (MalformedURLException e) {
				throw new IllegalArgumentException(url + " is malformed", e);
			}
		}
		return url;
	}

	static String getPath(String path) {
		if (StringUtils.hasText(path)) {
			path = path.trim();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
		}
		return path;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * bean注入的 入口函数
	 * @param metadata @EnableFeignClients 的注解消息
	 * @param registry
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		// 完成 Feign 框架相关的配置注册
		registerDefaultConfiguration(metadata, registry);
		// 注册由 @FeignClient 修饰的接口 bean *****核心*****
		registerFeignClients(metadata, registry);
	}

	/**
	 * 向容器中注册由 @FeignClient 修饰的接口bean
	 * @param metadata 包含了@EnableFeignClients 注解的元信息
	 * @param registry
	 */
	public void registerFeignClients(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		// ClassPath Scanner
		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.setResourceLoader(this.resourceLoader);
		// 接收 @EnableFeignClients(basePackages = {"com.xxx.feign"})
		Set<String> basePackages;
		Map<String, Object> attrs = metadata
				.getAnnotationAttributes(EnableFeignClients.class.getName());
		AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
				FeignClient.class);
		final Class<?>[] clients = attrs == null ? null
				: (Class<?>[]) attrs.get("clients");
		if (clients == null || clients.length == 0) {
			// 设置扫描的注解
			scanner.addIncludeFilter(annotationTypeFilter);
			// 获取 @EnableFeignClients中配置的 @FeignClient 接口扫描路径
			basePackages = getBasePackages(metadata);
		}
		else {
			final Set<String> clientClasses = new HashSet<>();
			basePackages = new HashSet<>();
			for (Class<?> clazz : clients) {
				basePackages.add(ClassUtils.getPackageName(clazz));
				clientClasses.add(clazz.getCanonicalName());
			}
			AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
				@Override
				protected boolean match(ClassMetadata metadata) {
					String cleaned = metadata.getClassName().replaceAll("\\$", ".");
					return clientClasses.contains(cleaned);
				}
			};
			scanner.addIncludeFilter(
					new AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
		}

		// 拿到 @EnableFeignClients 中配置的 @FeignClient 接口扫描路径 后开始 扫描
		for (String basePackage : basePackages) {
			// 查找 basePackage 包路径下所有 由 @FeignClient 修饰的候选bean,返回其 BeanDefinition 的集合
			Set<BeanDefinition> candidateComponents = scanner
					.findCandidateComponents(basePackage);

			// 针对每个标注了 @FeignClient 的候选 BeanDefinition (接口的BeanDefinition) 准备向容器中注册
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					// verify annotated class is an interface
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Assert.isTrue(annotationMetadata.isInterface(),
							"@FeignClient can only be specified on an interface");// @FeignClient标注的必须是接口
					// 获取 @FeignClient 注解的相关属性信息
					Map<String, Object> attributes = annotationMetadata
							.getAnnotationAttributes(
									FeignClient.class.getCanonicalName());
					// 获取@FeignClient(value ="服务名"), name属性和value属性是相同的含义, 都是配置服务名
					String name = getClientName(attributes);// name = hailtaxi-driver
					registerClientConfiguration(registry, name,
							attributes.get("configuration"));
					// 针对当前标注了 @FeignClient 注解的候选接口 BeanDefinition 向容器中注册bean信息
					registerFeignClient(registry, annotationMetadata, attributes);
				}
			}
		}
	}

	private void registerDefaultConfiguration(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		Map<String, Object> defaultAttrs = metadata
				.getAnnotationAttributes(EnableFeignClients.class.getName(), true);

		if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			}
			else {
				name = "default." + metadata.getClassName();
			}
			registerClientConfiguration(registry, name,
					defaultAttrs.get("defaultConfiguration"));
		}
	}

	/**
	 * 向容器中注册 每个标注了 @FeignClient 的接口bean
	 * @param registry
	 * @param annotationMetadata Feign 客户端类的所有注解信息
	 * @param attributes @FeignClient 注解的所有属性信息
	 */
	private void registerFeignClient(BeanDefinitionRegistry registry,
			AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
		// 接口全路径
		String className = annotationMetadata.getClassName();// className=com.xxx.feign.MyFeignService
		/**
		 * 每个标注了@FeignClient 的接口，真正向容器中注册的其实是一个 FeignClientFactoryBean
		 * 1、创建 FeignClientFactoryBean(FactoryBean)的 BeanDefinition
		 * 2、向 BeanDefinition中填充相关属性，属性来源于接口上@FeignClient的属性信息
		 * @see FeignClientFactoryBean#getObject()
		 */
		BeanDefinitionBuilder definition = BeanDefinitionBuilder
				.genericBeanDefinition(FeignClientFactoryBean.class);
		validate(attributes);// 验证fallback和fallbackFactory是不是接口

		// 将 FeignClient 注解信息 -> FeignClientFactoryBean 的bean定义中
		definition.addPropertyValue("url", getUrl(attributes));
		definition.addPropertyValue("path", getPath(attributes));
		String name = getName(attributes);
		definition.addPropertyValue("name", name);
		String contextId = getContextId(attributes);
		definition.addPropertyValue("contextId", contextId);
		definition.addPropertyValue("type", className); // 把接口全路径也设置到 definition，动态代理生成代理类
		definition.addPropertyValue("decode404", attributes.get("decode404"));
		definition.addPropertyValue("fallback", attributes.get("fallback"));
		definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

		String alias = contextId + "FeignClient";
		//
		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

		boolean primary = (Boolean) attributes.get("primary"); // has a default, won't be
																// null
		beanDefinition.setPrimary(primary);
		String qualifier = getQualifier(attributes);
		if (StringUtils.hasText(qualifier)) {
			alias = qualifier;
		}
		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className,
				new String[] { alias });
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
		/**
		 * 由于标注了@FeignClient的每个接口真正向容器中注册时注册的是与该接口相关的：FeignClientFactoryBean 而
		 * FeignClientFactoryBean 实现了 FactoryBean 接口，也就是说当需要从容器中获取这个bean时，
		 * 获取出来的bean其实是由它的getObject方法返回的bean
		 * 所以：下一个入口是： FeignClientFactoryBean#getObject
		 */
	}

	private void validate(Map<String, Object> attributes) {
		AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
		// This blows up if an aliased property is overspecified
		// FIXME annotation.getAliasedString("name", FeignClient.class, null);
		validateFallback(annotation.getClass("fallback"));
		validateFallbackFactory(annotation.getClass("fallbackFactory"));
	}

	/* for testing */ String getName(Map<String, Object> attributes) {
		String name = (String) attributes.get("serviceId");
		if (!StringUtils.hasText(name)) {
			name = (String) attributes.get("name");
		}
		if (!StringUtils.hasText(name)) {
			name = (String) attributes.get("value");
		}
		name = resolve(name);
		return getName(name);
	}

	private String getContextId(Map<String, Object> attributes) {
		String contextId = (String) attributes.get("contextId");
		if (!StringUtils.hasText(contextId)) {
			return getName(attributes);
		}

		contextId = resolve(contextId);
		return getName(contextId);
	}

	private String resolve(String value) {
		if (StringUtils.hasText(value)) {
			return this.environment.resolvePlaceholders(value);
		}
		return value;
	}

	private String getUrl(Map<String, Object> attributes) {
		String url = resolve((String) attributes.get("url"));
		return getUrl(url);
	}

	private String getPath(Map<String, Object> attributes) {
		String path = resolve((String) attributes.get("path"));
		return getPath(path);
	}

	/**
	 * 获取基于路径的 bean 扫描器
	 * @return
	 */
	protected ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
			@Override
			protected boolean isCandidateComponent(
					AnnotatedBeanDefinition beanDefinition) {
				boolean isCandidate = false;
				if (beanDefinition.getMetadata().isIndependent()) {
					if (!beanDefinition.getMetadata().isAnnotation()) {
						isCandidate = true;
					}
				}
				return isCandidate;
			}
		};
	}

	/**
	 * 获取 @EnableFeignClients中配置的 @FeignClient 接口扫描路径
	 * @param importingClassMetadata
	 * @return
	 */
	protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> attributes = importingClassMetadata
				.getAnnotationAttributes(EnableFeignClients.class.getCanonicalName());

		Set<String> basePackages = new HashSet<>();
		for (String pkg : (String[]) attributes.get("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : (String[]) attributes.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}

		if (basePackages.isEmpty()) {
			basePackages.add(
					ClassUtils.getPackageName(importingClassMetadata.getClassName()));
		}
		return basePackages;
	}

	private String getQualifier(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String qualifier = (String) client.get("qualifier");
		if (StringUtils.hasText(qualifier)) {
			return qualifier;
		}
		return null;
	}

	private String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = (String) client.get("contextId");
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("value");
		}
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("serviceId");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}

		throw new IllegalStateException("Either 'name' or 'value' must be provided in @"
				+ FeignClient.class.getSimpleName());
	}

	private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
			Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(FeignClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(
				name + "." + FeignClientSpecification.class.getSimpleName(),
				builder.getBeanDefinition());
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Helper class to create a {@link TypeFilter} that matches if all the delegates
	 * match.
	 *
	 * @author Oliver Gierke
	 */
	private static class AllTypeFilter implements TypeFilter {

		private final List<TypeFilter> delegates;

		/**
		 * Creates a new {@link AllTypeFilter} to match if all the given delegates match.
		 * @param delegates must not be {@literal null}.
		 */
		AllTypeFilter(List<TypeFilter> delegates) {
			Assert.notNull(delegates, "This argument is required, it must not be null");
			this.delegates = delegates;
		}

		@Override
		public boolean match(MetadataReader metadataReader,
				MetadataReaderFactory metadataReaderFactory) throws IOException {

			for (TypeFilter filter : this.delegates) {
				if (!filter.match(metadataReader, metadataReaderFactory)) {
					return false;
				}
			}

			return true;
		}

	}

}
