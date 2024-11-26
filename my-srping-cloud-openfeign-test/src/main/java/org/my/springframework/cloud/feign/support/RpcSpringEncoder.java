
package org.my.springframework.cloud.feign.support;

import feign.RequestTemplate;
import feign.codec.EncodeException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

@Component
public class RpcSpringEncoder extends SpringEncoder {

	public RpcSpringEncoder(ObjectFactory<HttpMessageConverters> messageConverters) {
		super(messageConverters);
	}

	@Override
	public void encode(Object requestBody, Type bodyType, RequestTemplate request)
			throws EncodeException {
		Map<String, Collection<String>> headers = request.headers();
		System.out.println(headers);
		super.encode(requestBody, bodyType, request);
	}
}
