package org.my.springframework.cloud.feign;

import org.my.springframework.cloud.feign.annotation.RequestObject;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

// AnnotatedParameterProcessor feign 方法参数注解处理器，总两个方法：1.获取当前参数注解类型; 2.处理当前参数
public class RequestObjectParameterProcessor implements AnnotatedParameterProcessor {
	private static final Class<RequestObject> ANNOTATION_TYPE = RequestObject.class;
	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION_TYPE;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
//		int parameterIndex = context.getParameterIndex();
//		Class<?> parameterType = method.getParameterTypes()[parameterIndex];
//		MethodMetadata methodMetadata = context.getMethodMetadata();
//		// 不是包装类或基本数据类型则对象处理
//		if (!ClassUtils.isPrimitiveOrWrapper(parameterType))  {
//			// 只能有一个 @RequestObject 或者 Map 或者 @QueryMap
//			Assert.isTrue(methodMetadata.queryMapIndex() == null, "只能有一个 @RequestObject 或者 Map 或者 @QueryMap");
//			methodMetadata.queryMapIndex(parameterIndex);
//		}
		return true;
	}
}
